#Purpose: losslessly (or near losslessly) compress an image using the CCSDS123 standard

# install pip
# then 'pip install scipy' get it

import numpy as np
import scipy.io         # loading .mat files
import matplotlib.pyplot as plt # visualization
import matplotlib.animation as animation
#Miscellaneous constants
dynamic_range = 32 #user-specified parameter between 2 and 32
s_min = -1*(2**(dynamic_range-1))
s_max = 2**(dynamic_range-1)
s_mid = 0
weight_resolution = 4 #user-specified parameter between 4 and 19
resolution = 4 # Can be any integer value from  0 to 4 
damping = 0 #Any integer value from 0 to 2^resolution - 1
offset = 0 #any integer value from 0 to 2^resolution -1
max_error = 0 #Max error is an array for each pixel in the image, but for now is used as a single variable
number_of_bands = 9 #user-defined parameter between 0 and 15 that indicates that number of previous bands used for prediction
register_size = 50 #user-defined parameter from max{32, 2^(D+weight_resolution+1)} to 64
v_min = -6 #vmin and vmax are user-defined parameters that control the rate at which the algorithm adapts to data statistics
v_max = 9 # -6 <= v_min < v_max <= 9
t_inc = (2**4) #parameter from 2^4 to 2^11
interband = 1 #interband and intraband offsets are used in updating of weight values (can be between -6 and 5)
intraband = 1
w_min = -(2**(weight_resolution+1)) #w_min and w_max values are used in weight updates (Equation 30)
w_max = 2**(weight_resolution+2) - 1

#Function required for weight update - not the same as numpy.sign so I had to quickly make it
def sign(x):
    if x >= 0:
        return 1
    else:
        return -1

#This mapper will take the quantized values and map them to unsigned integers
def mapper(s_hat, q, t, s_z):

    #Calculate theta (equation 56)
    if t == 0:
        theta = min(s_hat - s_min, s_max - s_hat)
    else:
        theta = min(np.floor((s_hat - s_min + max_error)/(2*max_error + 1)) , np.floor((s_max - s_hat + max_error)/(2*max_error + 1)))
    
    #Use theta to calculate Delta - the mapped quantizer index (equation 55)
    if abs(q)>theta:
        return abs(q) + theta
    elif 0 <= ((-1)**s_z)*q and ((-1)**s_z)*q <= theta:
        return 2*abs(q)
    else:
        return 2*abs(q) - 1

#Takes a sample and sample prediction and outputs a quantized value for the difference between the two
#Note: s_prev refers to s_z-1 (0), or the very first entry in the previous spectral band
def quantizer(s_hat,s, t, z):

    #First sample value for the first band
    if t == 0 and z == 0:
        return s_mid
    #First sample value for every other band
    if t == 0 and z > 0:
        return s - s_hat
    #For all t>0
    else:
        #Compute delta (residual)
        delta = s - s_hat
        #Return quantized delta
        return np.sign(delta)*np.floor((abs(delta) + max_error)/(2*max_error + 1))

#Calculates sample representative values for a given index, which are needed to calcuulate the next local sum in the image
def sample_rep_value(t, data, predicted_sample, quantized, hr_pred_sample_value):  

    #The quantizer value is clipped using equation 48
    clipped_quant = np.clip(predicted_sample + (quantized*(2*max_error+1)), s_min, s_max) 

    #If in the first sample in a band, the sample representative is equal to the data value
    if t == 0:
        sample_rep = data

    #Otherwise, calculations are made according to page 4-12
    else:
        #The double-resolution sample value is calculated next (Equation 47). Each section is a part of the complete equation
        section_one =  4*((2**resolution)-damping) 

        #Section 2 includes the clipped quantizer value and the sign of the original quantizer value
        section_two =  (clipped_quant*(2**weight_resolution)) - ((np.sign(quantized))*max_error*offset*(2**(weight_resolution - resolution)))

        #Section 3 includes the high-resolution predicted sample value calculated in section 4.7.2
        section_three = (damping*(hr_pred_sample_value)) - (damping*(2**(weight_resolution+1)))

        #The final double-resolution sample value:
        dr_sample_rep = np.floor((section_one*section_two + section_three)/(2**(resolution+weight_resolution+1)))

        #The sample rep value is calculated using the dr_sample_rep(Equation 46)
        sample_rep = np.floor((dr_sample_rep+1)/2)
    
    return sample_rep, clipped_quant

#Calculates a local sum for a pixel - note, value when x = 0 and y = 0 is not calculated, as it is not needed for prediction.
#Calculations are made using wide neighbor-oriented local sums specification (Equation 20 page 4-4)
def local_sums(x,y,z,Nx, sample_rep):
    
    # Calculation for the first row of a band
    if y==0 and x>0:
        local_sum = 4*(sample_rep[x-1,y,z])

    elif y>0:
        if x==0: #First column of a band
            local_sum = 2*(sample_rep[x,y-1,z] + sample_rep[x+1,y-1,z])

        elif x == (Nx-1): #Last column of a band
            local_sum = sample_rep[x-1,y,z] + sample_rep[x-1,y-1,z] + 2*sample_rep[x,y-1,z]

        else: # All other columns in the band
            local_sum = sample_rep[x-1,y,z] + sample_rep[x-1,y-1,z] + sample_rep[x,y-1,z] + sample_rep[x+1,y-1,z]

    elif y==0 and x ==0:
        local_sum = 0

    return local_sum

#Adds to the local difference vector, which is the difference between 4 times the sample representative value and the local sum
#Described on pages 4-5 to 4-6 of standard - note, a t value of 0 is not passed into the function (not needed)
#When this function is called, we will run a for loop for each band up to the number_of_bands constant
def local_diference_vector(x,y,z,sample_rep, local_sum, ld_vector):
    #if we're in the original band that the sample is 
    #When y == 0, the north, west, and northwest local differences are 0
    
    if y == 0:
        ld_vector = np.append(ld_vector, [0,0,0])
        
    #When x ==0, the local differences all have the same calculation
    elif x == 0:
        
        north_ld = 4*(sample_rep[x,y-1,z]) - local_sum
        ld_vector = np.append(ld_vector, [north_ld, north_ld, north_ld])
        
    #Otherwise, calculations from equations 25,26, and 27 are used
    else:
        
        north_ld = 4*(sample_rep[x,y-1,z]) - local_sum
        west_ld = 4*(sample_rep[x-1,y,z]) - local_sum
        northwest_ld = 4*(sample_rep[x-1,y-1,z]) - local_sum
        ld_vector = np.append(ld_vector, [north_ld, west_ld, northwest_ld])

    #If we're not in the original band (meaning we're in one of the previous bands used for prediction), 
    #only calculate central local difference
    for i in range(1,number_of_bands+1):
        
        #Equation 24
        central_ld = 4*(sample_rep[x,y,z-i]) - local_sum
        
        #Append the value to the local difference vector
        ld_vector = np.append(ld_vector, central_ld)
        
    #Return the new local difference vector
    return ld_vector

#Initializes the weight vector for t == 1 using default weight initialization.
#The complete vector will be generated by using a for loop to run through each previous band
def weight_initialization(weight_vector):

    #The north, west, and northwest weights are initialized as zero
    weight_vector = np.append(weight_vector, [0,0,0])
    
    #The first previous band is initialized according to equation 33(a)
    weight_one = (7/8)*(2**weight_resolution)
    weight_vector = np.append(weight_vector, weight_one)

    for i in range(1, number_of_bands):
    #The next bands, up until the final one used for prediction, are initialized using equation 33(b)
        w_length = len(weight_vector)
        weight_i = np.floor((1/8)*weight_vector[w_length-1])
        weight_vector = np.append(weight_vector, weight_i)

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
        if z==0:
            dr_sample_value = 2*s_mid
        else:
            dr_sample_value = 2*(data[x,y,z-1])
    
    #Otherwise, the hr_pred_sample_value is used to calculate it, according to equation 38
    else:
        dr_sample_value = np.floor(hr_pred_sample_value/(2**(weight_resolution+1)))
    
    #Lastly, the predicted sample value is half the dr_sample_value - equation 39
    pred_sample_value = dr_sample_value/2

    #Both the predicted and hr predicted sample value are returned - latter is used in sample rep calculation
    return pred_sample_value, hr_pred_sample_value, dr_sample_value


def weight_update(clipped_quant, dr_sample_value, t, Nx, weight_vector_prev, weight_vector, ld_vector):

    #Prediction error is calculated using equation 49s
    prediction_error = 2*clipped_quant - dr_sample_value
    

    #Next, the weight update scaling exponent is calculated, using user parameters of t_inc, v_min, and v_max (Equation 50)
    temp_1 = v_min + np.floor((t-Nx)/t_inc)
    weight_exponent = np.clip(temp_1, v_min, v_max) + dynamic_range - weight_resolution
    
    

    #The base calculation is used for all three values - Equations 52-54
    base = sign(prediction_error)*(2**(-(weight_exponent+intraband)))
    

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
        base_two = sign(prediction_error)*(2**(-(weight_exponent+interband)))

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
    
    #Stores all quantized values
    quantized = np.empty_like(data)

    #stores all predictions
    predictions = np.empty_like(data)

    #stores sample representative values
    sample_rep = np.empty_like(data)

    for z in range(0,Nz):
        print(z)
        for x in range(0,Nx):
            for y in range(0,Ny):
                t = y*(Nx) + x

                #Calculate local sums
                local = local_sums(x, y, z, Nx, sample_rep)


                ld_vector = np.empty(0)

                #Calculate the local difference vector
                ld_vector = local_diference_vector(x, y, z, sample_rep, local, ld_vector)
                
                #Initialize the weight vector if we're in the first pixel of a band
                if t == 0:
                    weight_vector_new = np.empty(0)
                    weight_vector_new = weight_initialization(weight_vector_new)
                
                #Calculate the predicted sample value, and other needed values (high resolution and double resolution predicted sample values)
                s_hat, hr_samp, dr_samp = prediction_calculation(ld_vector, weight_vector_new, local, t, x, y, z, data)

                #quantize the predicted sample value
                q = quantizer(s_hat, data[x,y,z], t, z)
                quantized[x,y,z] = q

                #Calculate the sample rep value for that pixel
                sample_rep[x,y,z], clipped_quantizer = sample_rep_value(t, data[x,y,z], s_hat, q, hr_samp)

                #Map the quantized values for the final prediction
                mapped = mapper(s_hat, q, t, dr_samp)

                #Add the final prediction to the storing variable to return
                predictions[x,y,z] = mapped

                #Assign the weight vector to be the previous one, and update the new one for the next pixel
                w_prev = weight_vector_new
                weight_vector_new = np.empty(0)
                weight_vector_new = weight_update(clipped_quantizer, dr_samp, t, Nx, w_prev, weight_vector_new, ld_vector)
      
    return predictions 

#Encodes the delta values from the predictor
def encoder(delta):

    encoded = []

    return encoded 



'''
#Load input data/image
# data = datalib.load_data_hdf5(path="images/indian_pines.mat", header="indian_pines")
data = scipy.io.loadmat("images/indian_pines.mat")
data = data[0:data_shape[0],0:data_shape[1],0:data_shape[2]]

#Run predictor
delta = predictor(data)
print("Here")
#Run encoder
#comp_image = encoder(delta)

#We need to write this encoded compressed image to a file -> need more research on this
'''

def test():

    # scripts for testing
    indian_pines = scipy.io.loadmat("images/Indian_pines.mat")

    # scipy.io.loadmat() returns dictionary of data
    print(indian_pines.keys())
    data = indian_pines['indian_pines']
    delta = predictor(data)