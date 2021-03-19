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
    
    return sample_rep, dr_sample_rep

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
            local_sum = sample_rep[x-1,y,z] + sample_rep[x-1,y-1,z] + sample_rep[x,y-1,z]

        else: # All other columns in the band
            local_sum = sample_rep[x-1,y,z] + sample_rep[x-1,y-1,z] + sample_rep[x,y-1,z] + sample_rep[x+1,y-1,z]

    return local_sum

#Adds to the local difference vector, which is the difference between 4 times the sample representative value and the local sum
#Described on pages 4-5 to 4-6 of standard - note, a t value of 0 is not passed into the function (not needed)
#When this function is called, we will run a for loop for each band up to the number_of_bands constant
def local_diference_vector(x,y,z,sample_rep, local_sum, band, ld_vector):

    #If we're not in the original band (meaning we're in one of the previous bands used for prediction), 
    #only calculate central local difference
    if band>0:

        #Equation 24
        central_ld = 4(sample_rep[x,y,z]) - local_sum
        #Append the value to the local difference vector
        ld_vector = np.append(ld_vector, central_ld)
        return ld_vector
    
    #Else, if we're in the original band that the sample is in
    else:
        #When y == 0, the north, west, and northwest local differences are 0
        if y == 0:
            ld_vector = np.append(ld_vector, [0,0,0])
        
        #When x ==0, the local differences all have the same calculation
        elif x == 0:
            north_ld = 4(sample_rep[x,y-1,z]) - local_sum
            ld_vector = np.append(ld_vector, [north_ld, north_ld, north_ld])
        
        #Otherwise, calculations from equations 25,26, and 27 are used
        else:
            north_ld = 4(sample_rep[x,y-1,z]) - local_sum
            west_ld = 4(sample_rep[x-1,y,z]) - local_sum
            northwest_ld = 4(sample_rep[x-1,y-1,z]) - local_sum
            ld_vector = np.append(ld_vector, [north_ld, ])
        
        #Return the new local difference vector
        return ld_vector

#Initializes the weight vector for t == 1 using default weight initialization.
#The complete vector will be generated by using a for loop to run through each previous band
def weight_initialization(band, weight_vector):

    #The north, west, and northwest weights are initialized as zero
    if band == 0:
        weight_vector = np.append(weight_vector, [0,0,0])
    
    #The first previous band is initialized according to equation 33(a)
    elif band == 1:
        weight_one = (7/8)*(2**weight_resolution)
        weight_vector = np.append(weight_vector, weight_one)

    #The next bands, up until the final one used for prediction, are initialized using equation 33(b)
    else:
        weight_i = np.floor((1/8)*weight_vector[end])
        weight_vector = np.append(weight_vector, weight_i)
    

    return weight_vector

#Computes a predicted sample value based on the local differences and weight vectors
def prediction_calculation(ld_vector, weight_vector, local_sum, t, z, data):

    #Inner product of local difference and weight vectors is taken , according to Equation 36
    pred_difference = np.inner(weight_vector, ld_vector))

    #Next, the high resolution predicted sample value is calculated according to equation 37
    #Here, it is broken up into several sections 
    section_one = pred_difference + (2**weight_resolution)*(local_sum-4*s_mid)

    #The mod function described in equation 4 is used in section two
    section_two = ((section_one+(2**(register_size-1))) % (2**register_size)) - (2**(register_size-1))

    #Lastly, the final parts of equation 36 prior to clipping are completed
    section_three = section_two + (2**(weight_resolution+2)*s_mid) + 2**(weight_resolution+1)

    #The min and max ranges for clipping are calculated
    min = (2**(weight_resolution +2)*s_min)
    max = (2**(weight_resolution +2)*s_max) + (2**(weight_resolution+1))

    #The hr_pred_sample value is section three clipped to the min and max ranges
    hr_pred_sample_value = np.clip(section_three, min, max)

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
    return pred_sample_value, hr_pred_sample_value



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