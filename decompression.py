#Purpose: losslessly (or near-losslessly) decompress an image which was compressed by the 
#         CCSDS123 standard compression.
import numpy as np

dynamic_range = 32
Nz = 220 #Will be passed from compressor

#Entropy encoder metadata that must be passed on from compressor:
u_max = 8
initial_count_exp = 1
accum_initial_constant = 0
gamma = 1 #stored as gamma - 4
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
        if (t>=Nz or t == 0): #If we've arrived at a new band, reset t, counter, and accum values
            t = 0
            counter = 2**initial_count_exp
            accum_value = np.floor((1/(2**7))*((3*(2**(k_zprime+6)))-49)*counter)

        if (2*counter>accum_value+np.floor((49/(2**7))*counter)): #Set code parameter
            code_param = 0
        else:
            for i in range(dynamic_range, 0, -1):
                if (counter*(2**i)<= accum_value+np.floor((49/(2**7))*counter)):
                    code_param = i
                    break  


        if encoded[i] == 1: #unary code is being read
            i+=1
            q+=1 #append to the unary variable
        elif encoded[i] == 0:
            i+=1 #skip the zero

            #Flag: a bit confused what to do here, as the length of the codeword depends on the value at that pixel, but we havent determined that value yet
                #Right now, the code is using the value from the previous pixel, which is not what is required
            if (np.floor(value/(2**code_param)) <u_max):
                remain = encoded[i:i+code_param] #read and convert the remainder
            else:
                remain = encoded[i:i+dynamic_range]

            r = int(str(remain), 2)

            value = q*code_param + r
            data.append(value)

            q = 0

            if (np.floor(value/(2**code_param)) <u_max):
                i += code_param #move to the next codeword, based on the length
            else:
                i += dynamic_range
            t += 1

            #Update counter and accumlator values for the next codeword
            if (counter< 2**gamma - 1):
                accum_value = accum_value + value
                counter = counter + 1
            elif (counter == 2**gamma - 1):
                accum_value = np.floor((accum_value + value +1)/2)
                counter = np.floor((counter+1)/2)
        
    data = np.array(data)    

    return data

def reverse_predictor():

    return 0


#Run the decoding algorithm
def main():
    return 0