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

import compression as comp

import numpy as np
import matplotlib.pyplot as plt
import scipy.io         # loading .mat files
# import matplotlib.pyplot as plt # visualization
# import matplotlib.animation as animation


# data is 3x3x3

Nx, Ny, Nz = 3,3,3

data_int = [[
		[6, 4, 2],
        [7, 8, 0],
        [4, 8, 5]],

       [[1, 2, 2],
        [6, 1, 6],
        [4, 6, 0]],

       [[1, 9, 2],
        [9, 5, 2],
        [4, 1, 5]]]
data_int = np.array(data_int)

data_binary = [
		[[0, 1, 0],
        [1, 1, 0],
        [1, 1, 1]],

       [[0, 0, 0],
        [1, 1, 0],
        [0, 0, 0]],

       [[1, 1, 0],
        [0, 0, 1],
        [1, 1, 1]]]
data_binary = np.array(data_binary)


def pictures():
	# scripts for testing
	indian_pines = scipy.io.loadmat("images/Indian_pines.mat")

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


def load_pic():
	data = scipy.io.loadmat("images/Indian_pines.mat")
	data = data['indian_pines'] # data is dictionary, only take the array part

	return data

def show_data(data, title):
	fig, axs = plt.subplots(2,2)
	axs[0,0].imshow(data[0,:,:], cmap="binary")
	axs[0,0].set_title(title + ": x slice 0")

	axs[0,1].set_title(title + ": x slice 1")
	axs[0,1].imshow(data[1,:,:], cmap="binary")

	axs[1,0].set_title(title + ": x slice 2")
	axs[1,0].imshow(data[2,:,:], cmap="binary")



# ------------------ running ---------------------
fig_count = 1
print("using data: \n", data_binary)


local_vector = np.zeros([Nx,Ny,Nz])

# data is in (z,y,x) format

for x in range (0,Nx):
	for y in range (0,Ny):
		for z in range (0,Nz):
			local = comp.local_sums(x,y,z,Nx, data_binary)
			print(x,y,z, " -> ", local)

			local_vector[x,y,z] = local


show_data(data_binary, "data_int")
show_data(local_vector, "local")

plt.show()