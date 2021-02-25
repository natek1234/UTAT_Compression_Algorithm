#Purpose: losslessly (or near losslessly) compress an image using the CCSDS123 standard

import numpy as np

#Predictor algorithm including Quantizer, Mapper, Sample Representative, and Prediction
def predictor(data):

    delta = []

    return delta

#Encodes the delta values from the predictor
def encoder(delta):

    encoded = []

    return encoded

#Runs the compression algorithm
def main():

    #Load input data/image
    data = datalib.load_data_hdf5(path="images/indian_pines.mat", header="indian_pines")
    data = data[0:data_shape[0],0:data_shape[1],0:data_shape[2]]

    #Run predictor
    delta = predictor(data)

    #Run encoder
    comp_image = encoder(delta)

    #We need to write this encoded compressed image to a file -> need more research on this


main()