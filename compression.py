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


#Calculates sample representative values for a given index, which are needed to calcuulate the next local sum in the image
def sample_rep_value(t, data, predicted_sample, quantized, hr_pred_sample_value):
     
     #If in the first sample in a band, the sample representative is equal to the data value
    if t == 0:
        sample_rep = data

    #Otherwise, calculations are made according to page 4-12
    else: 
        #The quantizer value is clipped using equation 48
        clipped_quant = np.clip(predicted_sample + (quantized*(2*max_error+1)), s_min, s_max)

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
    
    return sample_rep

'''Calculates a local sum for a pixel - note, value when x = 0 and y = 0 is not calculated, as it is not needed for prediction.
Calculations are made using wide neighbor-oriented local sums specification (Equation 20 page 4-4) '''
def local_sums(x,y,z,Nx, sample_rep):
    
    # Calculation for the first row of a band
    if y==0 and x>0:
        local_sum = 4*(sample_rep[x-1,y,z])

    elif y>0:
        if x==0: #First column of a band
            local_sum = 2*(sample_rep[x,y-1,z] + sample_rep[x+1,y-1,z])

        elif x == (Nx-1): #Last column of a band
            local_sum = sample_rep[x-1,y,z] + sample_rep[x-1,y-1,z] + sample_rep[x,y-1,z]

        else: # All other columns in the band
            local_sum = sample_rep[x-1,y,z] + sample_rep[x-1,y-1,z] + sample_rep[x,y-1,z] + sample_rep[x+1,y-1,z]

    return local_sum



#Predictor algorithm including Quantizer, Mapper, Sample Representative, and Prediction
def predictor(data):
    Nx = data.shape[0]
    Ny = data.shape[1]
    Nz = data.shape[2]



    delta = []
    return delta 

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

#Run encoder
comp_image = encoder(delta)

#We need to write this encoded compressed image to a file -> need more research on this
'''


# scripts for testing
indian_pines = scipy.io.loadmat("images/indian_pines.mat")

# scipy.io.loadmat() returns dictionary of data
print(indian_pines.keys())
data = indian_pines['indian_pines']

# data is 145x145x220
# first 2 axis is spatial, the last one (220) is spectral
# pictures looks like a shitty top down satellite image of supposedly "indian pines"

plt.figure(1)
plt.imshow(data[:,:,len(data[0,0])//2]) # plot spatial data, and the middle spectral data
plt.title("Middle of the spectrum")
plt.xlabel("x")
plt.ylabel("y")

plt.figure(2)
plt.imshow(data[:,:,len(data[0,0])-1]) # plot spatial data, and the middle spectral data
plt.title("end of the spectrum")
plt.xlabel("x")
plt.ylabel("y")

plt.figure(3)
plt.imshow(data[:,:,0]) # plot spatial data, and the middle spectral data
plt.title("start of the spectrum")
plt.xlabel("x")
plt.ylabel("y")

plt.show()

predictor(data)