#Purpose: losslessly (or near losslessly) compress an image using the CCSDS123 standard
#In this branch, quantizer and sample representative functions were removed in order to achieve completely lossless compression
#Each appearance of quantized values are replaced with the predicted residual

import numpy as np
import helperlib
#User-defined constants for predictor 
dynamic_range = 10 #user-specified parameter between 2 and 32
s_min = -1*(2**(dynamic_range-1))
s_max = 2**(dynamic_range-1)
s_mid = 0
weight_resolution = 4 #user-specified parameter between 4 and 19
resolution = 4 # Can be any integer value from  0 to 4 
damping = 0 #Any integer value from 0 to 2^resolution - 1
offset = 0 #any integer value from 0 to 2^resolution -1
max_error = 0 #Max error is an array for each pixel in the image, but for now is used as a single variable
number_of_bands = 2 #user-defined parameter between 0 and 15 that indicates that number of previous bands used for prediction
register_size = 50 #user-defined parameter from max{32, 2^(D+weight_resolution+1)} to 64
v_min = -6 #vmin and vmax are user-defined parameters that control the rate at which the algorithm adapts to data statistics
v_max = 9 # -6 <= v_min < v_max <= 9
t_inc = 2**4 #parameter from 2^4 to 2^11
interband = 1 #interband and intraband offsets are used in updating of weight values (can be between -6 and 5)
intraband = 1
w_min = -(2**(weight_resolution+1)) #w_min and w_max values are used in weight updates (Equation 30)
w_max = 2**(weight_resolution+2) - 1

#User-defined constants for encoder - in this implementation band sequential order was used, this changes the order pixels are inputted
output_word_size = 1 #measured in bytes - range one to eight
u_max = 8 #unary length limit - ranges between 8 and 32
initial_count_exp = 1 #initial count exponent used for adaptive statistics - ranges from 1 to 8
accum_initial_constant = 0 #user specified parameter from 0 to min(D-2,14)
gamma = 5 # used to rescale the counter - a value from 4 to 11
if (accum_initial_constant>30-dynamic_range):
    k_zprime = 2*accum_initial_constant + dynamic_range - 30
else:
    k_zprime = accum_initial_constant

#This mapper will take the predicted residuals and map them to unsigned integers
def mapper(predicted_sample, predicted_residual, t, dr_samp):

    #Calculate theta (equation 56)
    if t == 0:
        theta = min(predicted_sample - s_min, s_max - predicted_sample)
    else:
        theta = min(np.floor((predicted_sample - s_min + max_error)/(2*max_error + 1)) , np.floor((s_max - predicted_sample + max_error)/(2*max_error + 1)))
    
    #Use theta to calculate Delta - the mapped quantizer index (equation 55)
    if abs(predicted_residual)>theta:
        return abs(predicted_residual) + theta
    elif 0 <= ((-1)**dr_samp)*predicted_residual and ((-1)**dr_samp)*predicted_residual <= theta:
        return 2*abs(predicted_residual)
    else:
        return 2*abs(predicted_residual) - 1

#Calculates a local sum for a pixel - note, value when x = 0 and y = 0 is not calculated, as it is not needed for prediction.
#Calculations are made using wide neighbor-oriented local sums specification (Equation 20 page 4-4)
def local_sums(x,y,z,Nx, data):
    
    # Calculation for the first row of a band
    if y==0 and x>0:
        local_sum = 4*(data[z,y,x-1])

    elif y>0:
        if x==0: #First column of a band
            local_sum = 2*(data[z,y-1,x] + data[z,y-1,x+1])

        elif x == (Nx-1): #Last column of a band
            local_sum = data[z,y,x-1] + data[z,y-1,x-1] + 2*data[z,y-1,x]

        else: # All other columns in the band
            local_sum = data[z,y,x-1] + data[z,y-1,x-1] + data[z,y-1,x] + data[z,y-1,x+1]

    elif y==0 and x ==0:
        local_sum = 0

    return local_sum

#Adds to the local difference vector, which is the difference between 4 times the sample representative value and the local sum
#Described on pages 4-5 to 4-6 of standard - note, a t value of 0 is not passed into the function (not needed)
#When this function is called, we will run a for loop for each band up to the number_of_bands constant
def local_diference_vector(x,y,z,data, local_sum, ld_vector, Nz):

    #if we're in the original band that the sample is 
    #When y == 0, the north, west, and northwest local differences are 0
    if y==0:
        ld_vector = np.append(ld_vector, [0,0,0])

    #When x ==0, the local differences all have the same calculation
    elif x==0:
        north_ld = 4*(data[z,y-1,x]) - local_sum
        ld_vector = np.append(ld_vector, [north_ld, north_ld, north_ld])
        
    #Otherwise, calculations from equations 25,26, and 27 are used
    else:
        north_ld = 4*(data[z,y-1,x]) - local_sum
        west_ld = 4*(data[z,y,x-1]) - local_sum
        northwest_ld = 4*(data[z,y-1,x-1]) - local_sum
        ld_vector = np.append(ld_vector, [north_ld, west_ld, northwest_ld])
    
    #If we're not in the original band (meaning we're in one of the previous bands used for prediction), 
    #only calculate central local difference
    for i in range(1,number_of_bands+1):
        if (z+i<Nz):
            
            central_ld = 4*(data[z+i,y,x]) - local_sum
            ld_vector = np.append(ld_vector, central_ld)
        else:
            break
    
    return ld_vector

#Initializes the weight vector for t == 1 using default weight initialization.
#The complete vector will be generated by using a for loop to run through each previous band
def weight_initialization(weight_vector,z, Nz):

    #The north, west, and northwest weights are initialized as zero
    weight_vector = np.append(weight_vector, [0,0,0])
    
    #The first previous band is initialized according to equation 33(a)
    if (z != Nz-1):
        
        weight_one = (7/8)*(2**weight_resolution)
        weight_vector = np.append(weight_vector, weight_one)

    for i in range(2, number_of_bands+1):
    #The next bands, up until the final one used for prediction, are initialized using equation 33(b)
        if (z+i<Nz):
            w_length = len(weight_vector)
            weight_i = np.floor((1/8)*weight_vector[w_length-1])
            weight_vector = np.append(weight_vector, weight_i)
        else:
            break

    return weight_vector

#Computes a predicted sample value based on the local differences and weight vectors
def prediction_calculation(ld_vector, weight_vector, local_sum, t, x, y, z, data):

    #Inner product of local difference and weight vectors is taken , according to Equation 36
    pred_difference = np.inner(weight_vector, ld_vector)

    #Next, the high resolution predicted sample value is calculated according to equation 37
    #Here, it is broken up into several sections 
    section_one = pred_difference + (2**weight_resolution)*(local_sum-4*s_mid)

    #The mod function described in equation 4 is used in section two
    section_two = ((section_one+(2**(register_size-1))) % (2**register_size)) - (2**(register_size-1))

    #Lastly, the final parts of equation 36 prior to clipping are completed
    section_three = section_two + (2**(weight_resolution+2)*s_mid) + 2**(weight_resolution+1)

    #The min and max ranges for clipping are calculated
    minimum = (2**(weight_resolution +2)*s_min)
    maximum = (2**(weight_resolution +2)*s_max) + (2**(weight_resolution+1))

    #The hr_pred_sample value is section three clipped to the min and max ranges
    hr_pred_sample_value = np.clip(section_three, minimum, maximum)

    #Next, the double resolution sample value is calculated. When t == 0, this is the same as s_mid or the image data value
    if t == 0:
        if z==0 or number_of_bands == 0:
            dr_sample_value = 2*s_mid
        else:
            dr_sample_value = 2*(data[z-1,y,x])
    
    #Otherwise, the hr_pred_sample_value is used to calculate it, according to equation 38
    else:
        dr_sample_value = np.floor(hr_pred_sample_value/(2**(weight_resolution+1)))
    
    #Lastly, the predicted sample value is half the dr_sample_value - equation 39
    pred_sample_value = np.floor(dr_sample_value/2)

    #The difference between the actual value and the predicted value is calculated
    pred_residual = data[z,y,x] - pred_sample_value

    #Both the predicted and dr predicted sample value are returned - latter is used in updating the weight vector
    return pred_sample_value, pred_residual, dr_sample_value


def weight_update(dr_sample_value, predicted_sample, predicted_residual, t, Nx, weight_vector_prev, weight_vector, ld_vector,z,Nz):

    clipped_quant = np.clip(predicted_sample + (predicted_residual*(2*max_error+1)), s_min, s_max)

    #Prediction error is calculated using equation 49
    prediction_error = 2*clipped_quant - dr_sample_value

    #Next, the weight update scaling exponent is calculated, using user parameters of t_inc, v_min, and v_max (Equation 50)
    temp_1 = v_min + np.floor((t-Nx)/t_inc)
    weight_exponent = np.clip(temp_1, v_min, v_max) + dynamic_range - weight_resolution

    #The base calculation is used for all three values - Equations 52-54
    base = (helperlib.sign(prediction_error))*(2**(-(weight_exponent+intraband)))

    #The temporary north, west, and northwest values are calculated using the previous weight vector
    temp_n = weight_vector_prev[0] + np.floor((1/2)*(base*ld_vector[0] +1))
    temp_w = weight_vector_prev[1] + np.floor((1/2)*(base*ld_vector[1] +1))
    temp_nw = weight_vector_prev[2] + np.floor((1/2)*(base*ld_vector[2] +1))

    #The temporary values are clipped to the user-defined min-max range for weight
    updated_n = np.clip(temp_n, w_min, w_max)
    updated_w = np.clip(temp_w, w_min, w_max)
    updated_nw = np.clip(temp_nw, w_min, w_max)

    #These values are added to the new weight_vector for the new t value
    weight_vector = np.append(weight_vector, [updated_n, updated_w, updated_nw])

    for i in range(1, number_of_bands+1):

        #The same base calculation is used - now with the interband user parameter
        base_two = helperlib.sign(prediction_error)*(2**(-(weight_exponent+interband)))
        if (z+i<Nz):
            #band+2 is used as the index, since three values for north, west, and northwest weights are at the front of the vector
            temp_z = weight_vector_prev[i+2] + np.floor((1/2)*(base_two*ld_vector[i+2] + 1))
        
            #A new updated weight is calculated using equation 51
            updated_z = np.clip(temp_z, w_min, w_max)

            #This weight value is appended to the new weight vector
            weight_vector = np.append(weight_vector, updated_z)
            
    
    return weight_vector
        


#Predictor algorithm including Quantizer, Mapper, Sample Representative, and Prediction
def predictor(data):
    
    #Grab data shape dimensions
    Nx = data.shape[0]
    Ny = data.shape[1]
    Nz = data.shape[2]
    

    #stores all predictions
    predictions = np.empty_like(data)

    for z in range(0,Nz):
        for x in range(0,Nx):
            for y in range(0,Ny):
                t = y*(Nx) + x

                #Calculate local sum at that pixel
                local = local_sums(x, y, z, Nx, data)

                ld_vector = np.empty(0)

                #Calculate the local difference vector
                ld_vector = local_diference_vector(x, y, z, data, local, ld_vector, Nz)
                
                #Initialize the weight vector if we're in the first pixel of a band
                if t == 0:
                    weight_vector_new = np.empty(0)
                    weight_vector_new = weight_initialization(weight_vector_new, z, Nz)
                
                #Calculate the predicted residual, and other needed values (high resolution and double resolution predicted sample values)
                predicted_sample, predicted_residual, dr_samp = prediction_calculation(ld_vector, weight_vector_new, local, t, x, y, z, data)

                #Map the predicted residual for the final prediction and store it in the return array
                predictions[z,y,x] = mapper(predicted_sample, predicted_residual, t, dr_samp)

        
                #Assign the weight vector to be the previous one, and update the new one for the next pixel
                w_prev = weight_vector_new
                weight_vector_new = np.empty(0)
                weight_vector_new = weight_update(dr_samp, predicted_sample, predicted_residual, t, Nx, w_prev, weight_vector_new, ld_vector,z,Nz)
      
    return predictions 



#Encodes the delta values from the predictor
#encoded image consists of a header followed by a body
#   Header describes image and compression parameters for decompression
#Two options for encoder: sample- adaptive and block-adaptive: this approach uses sample-adaptive encoding
def encoder(delta):
    Nz = delta.shape[2]
    Ny = delta.shape[1]
    Nx = delta.shape[0]
    encoded = []

    
    for z in range(0, Nz):
        #Set initial counter and accumulator values for the band 
        counter = 2**initial_count_exp
        accum_value = np.floor((1/(2**7))*((3*(2**(k_zprime+6)))-49)*counter) #Equation 58
        for y in range(0,Ny):
            for x in range(0,Nx):
                t = y*Nx + x
                #At the first pixel, the endoced value is just the D-bit representation of delta
                if (t==0):
                    code = helperlib.dec_to_bin(delta[z,y,x], dynamic_range)
                    encoded += code
                else:
                    #Using the adaptive code statistics, set the code parameter, according to equation 62 in section 5.4.3.2.4
                    if (2*counter>accum_value+np.floor((49/(2**7))*counter)):
                        code_param = 0
                    else:
                        
                        for i in range(dynamic_range, 0, -1):
                            if (counter*(2**i)<= accum_value+np.floor((49/(2**7))*counter)):
                                code_param = i
                                break
                    
                    #Use golomb power of two code words to write a binary codeword, based on the user-defined unary length limit
                    if (np.floor(delta[z,y,x]/(2**code_param))<u_max):
                        
                        #Write unary code
                        u = [1] * int(np.floor(delta[z,y,x]/(2**code_param)))
                        u.append(0)
                        
                        #Write remainder code
                        r = helperlib.dec_to_bin(delta[z,y,x], dynamic_range)
                        if (code_param ==0):
                            encoded+=u
                            
                        else:
                            
                            r = r[-code_param:]
                            
                            encoded+=u+r
                            
                        
                        
                        
                        
                        
                    else:
                        
                        #Unary code
                        u = [1] * u_max
                        u.append(0)
                        
                        #Remainder code
                        r = helperlib.dec_to_bin(delta[z,y,x], dynamic_range)

                        encoded += u + r
                        
            
                #Update counter and accumulator values after each pixel, according to section 5.4.3.2.3
                if (t>=1):
                    if (counter< 2**gamma - 1):
                        accum_value = accum_value + delta[z,y,x]
                        counter = counter + 1
                    elif (counter == 2**gamma - 1):
                        accum_value = np.floor((accum_value + delta[z,y,x] +1)/2)
                        counter = np.floor((counter+1)/2)

    
    return encoded 




