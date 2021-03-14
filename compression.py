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

#Calculates sample representative values for a given index, which are needed to calcuulate the next local sum in the image
def sample_rep_value(z,y,x):
    t = y*Nx + x
    if t == 0:
        sample_rep = data[z,y,x]
    else:
        quant_clipped = np.clip((predicted_sample[z,y,x] + (quantized[z,y,x])*(2*max_error[z,y,x]+1)), s_min, s_max)

        dr_sample_rep = 4*((2**resolution)-damping) * (quant_clipped*(2**weight_resolution) - (np.sign(quantized[z,y,x])*max_error[z,y,x]*(2**(weight_resolution - resolution)))) 
        + (damping*(hr_pred_sample_value[z,y,x])) - (damping*(2**(weight_resolution+1)))

        sample_rep = (dr_sample_rep+1)/2

    return sample_rep

#Calculates local sums based on a given index - note, value when x = 0 and y = 0 is not calculated, as it is not needed for prediction
def local_sums(z,y,x):

    if y==0 and x>0:
        local_sum = 4*(sample_rep[z,y,x-1])

    elif y>0:
        if x==0:
            local_sum = 2*(sample_rep[z,y-1,x] + sample_rep[z,y-1,x+1])
        elif x == (Nx-1):
            local_sum = sample_rep[z,y,x-1] + sample_rep[z,y-1,x-1] + sample_rep[z,y-1,x]
        else:
            local_sum = sample_rep[z,y,x-1] + sample_rep[z,y-1,x-1] + sample_rep[z,y-1,x] + sample_rep[z,y-1,x+1]
    return local_sum

#Predictor algorithm including Quantizer, Mapper, Sample Representative, and Prediction
def predictor(data):
    Nx = data.shape[2]
    Ny = data.shape[1]
    Nz = data.shape[0]


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
