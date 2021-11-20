#Purpose: losslessly (or near-losslessly) decompress an image which was compressed by the 
#         CCSDS123 standard compression.
import numpy as np
import helperlib
import compression as comp

dynamic_range = 10
Nx =3
Ny = 3
Nz = 3 #Will be passed from compressor
s_min = -1*(2**(dynamic_range-1))
s_max = 2**(dynamic_range-1)
s_mid = 0

#Entropy encoder metadata that must be passed on from compressor:
u_max = 8
initial_count_exp = 1
accum_initial_constant = 0
gamma = 5
if (accum_initial_constant>30-dynamic_range):
    k_zprime = 2*accum_initial_constant + dynamic_range - 30
else:
    k_zprime = accum_initial_constant

def decode(encoded):
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
            value = encoded[i:i+dynamic_range]
            r = helperlib.bin_to_dec(value)
            
            data.append(r)
            i+=dynamic_range
            t+=1
            continue

        #Else if t is not zero
        if (2*counter>accum_value+np.floor((49/(2**7))*counter)): #Set code parameter
            code_param = 0
        else:    
            for j in range(dynamic_range, 0, -1):
                if (counter*(2**j)<= accum_value+np.floor((49/(2**7))*counter)):
                    code_param = j
                    break  
        

        while(encoded[i] == 1): #unary code is being read
            i+=1
            q+=1 #append to the unary variable
        if encoded[i] == 0:
            
            i+=1 #skip the zero
            
            
            if (q == u_max):
                remain = encoded[i:i+dynamic_range]
                i+=dynamic_range
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
     
    data.shape = (3,3,3)

    return data

def unmap(predicted_sample, mapped):

    #Calculate the value of theta using the predicted sample - this code assumes max_error is 0 (which we currently have it set to),
    #but can be modified to accomadate for max error (depending on t).
    theta = predicted_sample - s_min
    select = True
    if (theta > s_max - predicted_sample):
        theta = s_max - predicted_sample
        select = False
    
    dr_samp = 2*predicted_sample

    #If mapped depends on theta, we make this calculation
    if (mapped > 2*theta):
        if (select):
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
            delta = (sign)*(mapped+1/2)

    #Since sample - predicted = delta
    sample = delta + predicted_sample
    return sample


#Run the reverse prediction algorithm
def unpredict(mapped):


    return 0