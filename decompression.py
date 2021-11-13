#Purpose: losslessly (or near-losslessly) decompress an image which was compressed by the 
#         CCSDS123 standard compression.
import numpy as np
import helperlib

dynamic_range = 10
Nx =3
Ny = 3
Nz = 3 #Will be passed from compressor

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

def reverse_predictor():

    return 0


#Run the decoding algorithm
def main():
    return 0