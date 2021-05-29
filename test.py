'''
Yong Da Li
Saturday, May 15, 2021

writing some tests to check the helper functions in
Jasnoor's and Ketan's CCSDS compression

blue books: https://public.ccsds.org/publications/bluebooks.aspx
- Low-Complexity Lossless and Near-Lossless Multispectral and Hyperspectral Image Compression
- they released like different versions
- back in November, we had c2, now they released c3
'''

import logging
logging.basicConfig(level=logging.DEBUG)

import compression as comp

import numpy as np
import scipy.io         # loading .mat files
# import matplotlib.pyplot as plt # visualization
# import matplotlib.animation as animation

def pictures():
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


# ------------------ running ---------------------

data = scipy.io.loadmat("images/indian_pines.mat")
data = data['indian_pines'] # data is dictionary, only take the array part

s_hat = None
s_prev = None
t,x,y,z = 0,0,0,0

q = comp.quantizer(s_hat, data[x,y,z], t, z, s_prev)
assert(q==0)

t,x,y,z = 1,1,1,1
q = comp.quantizer(s_hat, data[x,y,z], t, z, s_prev)



# pictures()


