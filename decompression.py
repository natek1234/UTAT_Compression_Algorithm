#Purpose: losslessly (or near-losslessly) decompress an image which was compressed by the 
#         CCSDS123 standard compression.
#Current issue: to run the prediction algorithm again on the ground, we must perform a local difference calculation. However, 
#the local difference calculation depends on the original data values in previous spectral bands, which we've not calculated
#yet as we move through the image. Need to read through C implementation to understand how this step is performed.
import numpy as np
import helperlib
import compression as comp



s_min = -1*(2**(comp.dynamic_range-1))
s_max = 2**(comp.dynamic_range-1)
s_mid = 0

#Entropy encoder metadata that must be passed on from compressor:
u_max = 8
initial_count_exp = 1
accum_initial_constant = 0
gamma = 5
if (accum_initial_constant>30-comp.dynamic_range):
    k_zprime = 2*accum_initial_constant + comp.dynamic_range - 30
else:
    k_zprime = accum_initial_constant

def decode(encoded, Nz, Ny, Nx):
    data = []
    i = 0
    q = 0
    t = 0 #Keep track of position for re-setting accumulator
    
    while i < len(encoded):
        
        if (t>=(Nx*Ny)):
            t=0  
            continue

        if (t == 0): #If we've arrived at a new band, reset t, counter, and accum values
            counter = 2**initial_count_exp
            accum_value = np.floor((1/(2**7))*((3*(2**(k_zprime+6)))-49)*counter)
            value = encoded[i:i+comp.dynamic_range]
            
            r = helperlib.bin_to_dec(value)
            
            data.append(r)
            i+=comp.dynamic_range
            t+=1
            continue

        #Else if t is not zero
        if (2*counter>accum_value+np.floor((49/(2**7))*counter)): #Set code parameter
            code_param = 0
        else:    
            for j in range(comp.dynamic_range, 0, -1):
                if (counter*(2**j)<= accum_value+np.floor((49/(2**7))*counter)):
                    code_param = j
                    break  
        

        while(encoded[i] == 1): #unary code is being read
            i+=1
            q+=1 #append to the unary variable
        if encoded[i] == 0:
            
            i+=1 #skip the zero
            
            
            if (q == u_max):
                remain = encoded[i:i+comp.dynamic_range]
                i+=comp.dynamic_range
                r = helperlib.bin_to_dec(remain)
                value = r
            else:
                if (code_param != 0):
                    
                    remain = encoded[i:i+code_param]
                    
                    i+=code_param
                    r = helperlib.bin_to_dec(remain)
                    value = q*(2**code_param) + r
                else:
                    value = q
           
            
            
            data.append(value)
            
            q = 0
            t += 1

            #Update counter and accumlator values for the next codeword
            if (counter< 2**gamma - 1):
                accum_value = accum_value + value
                counter = counter + 1
            elif (counter == 2**gamma - 1):
                accum_value = np.floor((accum_value + value +1)/2)
                counter = np.floor((counter+1)/2)
     
    data = np.array(data)
     
    data= np.reshape(data, (Nz, Ny, Nx))
    
    return data


def unmap(predicted_sample, mapped, dr_samp):
    
    #Calculate the value of theta using the predicted sample - this code assumes max_error is 0 (which we currently have it set to),
    #but can be modified to accomadate for max error (depending on t).
    theta = min(predicted_sample-s_min, s_max -predicted_sample)
    
    

    #If mapped depends on theta, we make this calculation
    if (mapped > 2*theta):
        if (theta == predicted_sample - s_min):
            delta = mapped - theta
        else:
            delta = theta - mapped
        
    #Otherwise, based on if mapped is stored as an even or odd value, we make a calculation
    else:
        if(mapped % 2 == 0):
            if (dr_samp %2 == 0):
                sign = 1
            else:
                sign = -1
            delta = (sign)*(mapped/2)
        else:
            if (dr_samp %2 == 0):
                sign = -1
            else:
                sign = 1
            delta = (sign)*((mapped+1)/2)
        

    #Since sample - predicted = delta
    sample = delta + predicted_sample
    return sample, delta

def unpredict(mapped, Nz, Ny, Nx):
    data = np.zeros_like(mapped)
    for z in range(Nz-1, -1,-1):
        for y in range(0, Ny):
            for x in range(0, Nx):
                
                t = y*(Nx) + x
                
                if (t==0):
                    #Initialize the weight vector if we're in the first pixel of a band
                    weight_vector_new = np.empty(0)
                    weight_vector_new = comp.weight_initialization(weight_vector_new, z, Nz)
                    data[z,y,x], predicted_residual = unmap(s_mid, mapped[z,y,x], s_mid*2)
                    continue
                
                
                #Calculate local sum at that pixel
                local = comp.local_sums(x, y, z, Nx, data)
                
                ld_vector = np.empty(0)

                #Calculate the local difference vector
                ld_vector = comp.local_diference_vector(x, y, z, data, local, ld_vector, Nz)
                
                
                #Calculate the predicted residual, and other needed values (high resolution and double resolution predicted sample values)
                predicted_sample, discard, dr_samp = comp.prediction_calculation(ld_vector, weight_vector_new, local, t, x, y, z, data)
                
                
                #Using the unmap fucntion, compute the 
                data[z,y,x], predicted_residual = unmap(predicted_sample, mapped[z,y,x], dr_samp)

                #Assign the weight vector to be the previous one, and update the new one for the next pixel
                w_prev = weight_vector_new
                weight_vector_new = np.empty(0)
                weight_vector_new = comp.weight_update(dr_samp, predicted_sample, predicted_residual, t, Nx, w_prev, weight_vector_new, ld_vector,z,Nz)
                
        #reorderBands(data, Nz)
    return data


def reorderBands(bands, Nz):
    tempBand = bands[0]
    for i in range(0, Nz-1):
        bands[i] = bands[i+1]
    
    bands[Nz-1] = tempBand