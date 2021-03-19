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
def quantizer(s_hat,s, t, z, s_prev):

    #First sample value for the first band
    if t == 0 and z == 0:
        return s_mid
    #First sample value for every other band
    if t == 0 and z > 0:
        return s_prev
    #For all t>0
    else:
        #Compute delta (residual)
        delta = s - s_hat
        #Return quantized delta
        return np.sign(delta)*np.floor( (abs(delta) + max_error)/(2*max_error + 1) )


#Calculates sample representative values for a given index, which are needed to calculate the next local sum in the image
def sample_rep_value(z,y,x, Nx):
    t = y*(Nx) + x
    
    if t == 0:
        sample_rep = data[z,y,x]
    else:
        quant_clipped = np.clip((predicted_sample[z,y,x] + ((quantized[z,y,x])*(2*max_error+1))), s_min, s_max)

        dr_sample_rep = 4*((2**resolution)-damping) * (quant_clipped*(2**weight_resolution) - (np.sign(quantized[z,y,x])*max_error*offset*(2**(weight_resolution - resolution))))
        + (damping*(hr_pred_sample_value[z,y,x])) - (damping*(2**(weight_resolution+1)))

        sample_rep = np.floor((dr_sample_rep+1)/2)

    return sample_rep

#Calculates local sums based on a given index - note, value when x = 0 and y = 0 is not calculated, as it is not needed for prediction
def local_sums(z,y,x, Nx):

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
    #Grabe data shape dimensions
    Nx = data.shape[0]
    Ny = data.shape[1]
    Nz = data.shape[2]

    #Initialized predictor variables
    s_hat = None
    s_prev = None

    #Stores all quantized values
    quantized = np.empty_like(data)

    #stores all predictions
    predictions = np.empty_like(data)

    for z in range(0,Nz):
        for x in range(0,Nx):
            for y in range(0,Ny):
                t = y*(Nx) + x
                q = quantizer(s_hat, data[x,y,z], t, z, s_prev)
                quantized[x,y,z] = q

                #JASNOOR FUNCTION CALLS HERE - include a return for s_z which is high resolution value
                s_z = None #temporary placeholder

                mapped = mapper(s_hat, q, t, s_z)
                #Set s_prev before
        if z > 0:
            s_prev = predictions[0,0,z]



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
