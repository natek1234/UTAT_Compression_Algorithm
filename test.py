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
import decompression as decomp

import numpy as np
import matplotlib.pyplot as plt
import scipy.io

from helperlib import dec_to_bin         # loading .mat files
# import matplotlib.pyplot as plt # visualization
# import matplotlib.animation as animation


# data is 3x3x3



data_one = [[
		[6, 4, 2],
        [7, 8, 0],
        [4, 8, 5]],

       [[1, 2, 2],
        [6, 1, 6],
        [4, 6, 0]],

       [[1, 9, 2],
        [9, 5, 2],
        [4, 1, 5]]]
data_one = np.array(data_one)

data_two = np.random.randint(0, 100, 1000)
data_two = np.reshape(data_two, (10,10,10))


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


#In CCSDS, y defines the row and x defines the column
def local_sums_visualize(data):
	Nx = data.shape[2]
	Ny = data.shape[1]
	Nz = data.shape[0]
	local_vector = np.zeros([Nz,Ny,Nx])
	print("Local Sums:")
	for z in range (0,Nz):
		for y in range (0,Ny):
			for x in range (0,Nx):
				local = comp.local_sums(x,y,z,Nx, data)
				print(x,y,z, " -> ", local)

				local_vector[z,y,x] = local
				


	show_data(data, "data")
	show_data(local_vector, "local")
	
	plt.show()

def diff_vector_visualize(data):
	print("Local Difference Vectors:")
	Nx = data.shape[2]
	Ny = data.shape[1]
	Nz = data.shape[0]
	diff_vector = np.zeros([Nz,Ny,Nx])
	for z in range(0,Nz):
		for y in range(0,Ny):
			for x in range(0,Nx):
				ld_vector = np.empty(0)
				local = comp.local_sums(x,y,z,Nx,data)
				ld_vector = comp.local_diference_vector(x,y,z,data,local,ld_vector, Nz)

				print(x,y,z, "->", ld_vector)
				diff_vector[z,y,x] = np.linalg.norm(ld_vector)
	
	show_data(diff_vector, "local difference")
	plt.show()
	
def weight_vector_visualize(data):
	print("Weight Vector Initializations:")
	Nx = data.shape[2]
	Ny = data.shape[1]
	Nz = data.shape[0]
	for z in range(0,Nz):
		weight_vector_new = np.empty(0)
		weight_vector_new = comp.weight_initialization(weight_vector_new,z,Nz)
		print(z, "->", weight_vector_new) 


def prediction_calculation_visualize(data):
	print("Predicted Residuals: ")
	Nx = data.shape[2]
	Ny = data.shape[1]
	Nz = data.shape[0]
	predicted_residuals = np.zeros([Nz,Ny,Nx])

	for z in range(0,Nz):
		for y in range(0,Ny):
			for x in range(0,Nx):
				t= x + y*Nx

				local = comp.local_sums(x,y,z,Nx,data)
				ld_vector = np.empty(0)
				ld_vector = comp.local_diference_vector(x,y,z,data,local,ld_vector, Nz)

				if (t==0):
					weight_vector_new = np.empty(0)
					weight_vector_new = comp.weight_initialization(weight_vector_new,z,Nz)
				
				predicted_sample, predicted_residual, dr_samp = comp.prediction_calculation(ld_vector, weight_vector_new, local, t, x, y, z, data)
				print(x,y,z, "-> ", data[z,y,x], predicted_residual)
				predicted_residuals[z,y,x]=predicted_residual

				w_prev = weight_vector_new
				weight_vector_new=np.empty(0)
				weight_vector_new=comp.weight_update(dr_samp,predicted_sample,predicted_residual,t,Nx,w_prev,weight_vector_new,ld_vector,z,Nz)
	show_data(data, "data")
	show_data(predicted_residuals, "predicted residuals") 
	plt.show()

def weight_vector_update_visualize(data):
	print("Weight vectors: (for t+1): ")
	Nx = data.shape[2]
	Ny = data.shape[1]
	Nz = data.shape[0]
	for z in range(0,Nz):
		for y in range(0,Ny):
			for x in range(0,Nx):
				t= x + y*Nx

				local = comp.local_sums(x,y,z,Nx,data)
				ld_vector = np.empty(0)
				ld_vector = comp.local_diference_vector(x,y,z,data,local,ld_vector, Nz)

				if (t==0):
					weight_vector_new = np.empty(0)
					weight_vector_new = comp.weight_initialization(weight_vector_new,z,Nz)
				
				predicted_sample, predicted_residual, dr_samp = comp.prediction_calculation(ld_vector, weight_vector_new, local, t, x, y, z, data)

				w_prev = weight_vector_new
				weight_vector_new=np.empty(0)
				weight_vector_new=comp.weight_update(dr_samp,predicted_sample,predicted_residual,t,Nx,w_prev,weight_vector_new,ld_vector,z,Nz)
				print(t,"->", weight_vector_new)


def mapper_visualize(data):
	print("Mapper values:")
	Nx = data.shape[2]
	Ny = data.shape[1]
	Nz = data.shape[0]
	mapper = np.zeros([Nz, Ny, Nx])
	for z in range(0,Nz):
		for y in range(0,Ny):
			for x in range(0,Nx):
				t= x + y*Nx

				local = comp.local_sums(x,y,z,Nx,data)
				ld_vector = np.empty(0)
				ld_vector = comp.local_diference_vector(x,y,z,data,local,ld_vector, Nz)

				if (t==0):
					weight_vector_new = np.empty(0)
					weight_vector_new = comp.weight_initialization(weight_vector_new,z,Nz)
				
				predicted_sample, predicted_residual, dr_samp = comp.prediction_calculation(ld_vector, weight_vector_new, local, t, x, y, z, data)
				mapper[z,y,x] = comp.mapper(predicted_sample, predicted_residual, t, dr_samp)
				print(x,y,z, "->", mapper[z,y,x])
				w_prev = weight_vector_new
				weight_vector_new=np.empty(0)
				weight_vector_new=comp.weight_update(dr_samp,predicted_sample,predicted_residual,t,Nx,w_prev,weight_vector_new,ld_vector,z,Nz)
	show_data(mapper, "Mapped values")
	plt.show()

def encoder_visualize(data):
	Nx = data.shape[2]
	Ny = data.shape[1]
	Nz = data.shape[0]
	mapped = np.zeros([Nz,Ny,Nx])
	encoded = []
	for z in range(0,Nz):
		for y in range(0,Ny):
			for x in range(0,Nx):
				t= x + y*Nx

				local = comp.local_sums(x,y,z,Nx,data)
				ld_vector = np.empty(0)
				ld_vector = comp.local_diference_vector(x,y,z,data,local,ld_vector, Nz)

				if (t==0):
					weight_vector_new = np.empty(0)
					weight_vector_new = comp.weight_initialization(weight_vector_new,z,Nz)
				
				predicted_sample, predicted_residual, dr_samp = comp.prediction_calculation(ld_vector, weight_vector_new, local, t, x, y, z, data)
				mapped[z,y,x] = comp.mapper(predicted_sample, predicted_residual, t, dr_samp)

				w_prev = weight_vector_new
				weight_vector_new=np.empty(0)
				weight_vector_new=comp.weight_update(dr_samp,predicted_sample,predicted_residual,t,Nx,w_prev,weight_vector_new,ld_vector,z,Nz)
	print(mapped)
	encoded = comp.encoder(mapped)
	print(encoded)

def decoder_visualize(data):
	Nx = data.shape[2]
	Ny = data.shape[1]
	Nz = data.shape[0]
	mapped = np.zeros([Nz,Ny,Nx])
	encoded = []
	for z in range(0,Nz):
		for y in range(0,Ny):
			for x in range(0,Nx):
				t= x + y*Nx

				local = comp.local_sums(x,y,z,Nx,data)
				ld_vector = np.empty(0)
				ld_vector = comp.local_diference_vector(x,y,z,data,local,ld_vector, Nz)

				if (t==0):
					weight_vector_new = np.empty(0)
					weight_vector_new = comp.weight_initialization(weight_vector_new,z,Nz)
				
				predicted_sample, predicted_residual, dr_samp = comp.prediction_calculation(ld_vector, weight_vector_new, local, t, x, y, z, data)
				mapped[z,y,x] = comp.mapper(predicted_sample, predicted_residual, t, dr_samp)

				w_prev = weight_vector_new
				weight_vector_new=np.empty(0)
				weight_vector_new=comp.weight_update(dr_samp,predicted_sample,predicted_residual,t,Nx,w_prev,weight_vector_new,ld_vector,z,Nz)
	print(mapped)
	encoded = comp.encoder(mapped)
	decoded = decomp.decode(encoded)
	print(decoded)

def unpredict_visualize(data):
	Nx = data.shape[2]
	Ny = data.shape[1]
	Nz = data.shape[0]
	mapped = np.zeros([Nz,Ny,Nx])
	data_final = np.zeros([Nz,Ny,Nx])
	encoded = []
	for z in range(0,Nz):
		for y in range(0,Ny):
			for x in range(0,Nx):
				t= x + y*Nx

				local = comp.local_sums(x,y,z,Nx,data)

				ld_vector = np.empty(0)
				ld_vector = comp.local_diference_vector(x,y,z,data,local,ld_vector, Nz)

				if (t==0):
					weight_vector_new = np.empty(0)
					weight_vector_new = comp.weight_initialization(weight_vector_new,z,Nz)
				
				predicted_sample, predicted_residual, dr_samp = comp.prediction_calculation(ld_vector, weight_vector_new, local, t, x, y, z, data)
				
				mapped[z,y,x] = comp.mapper(predicted_sample, predicted_residual, t, dr_samp)
				
				if (t!=0):
					w_prev = weight_vector_new
					weight_vector_new=np.empty(0)
					weight_vector_new=comp.weight_update(dr_samp,predicted_sample,predicted_residual,t,Nx,w_prev,weight_vector_new,ld_vector,z,Nz)
	
	encoded = comp.encoder(mapped)
	decoded = decomp.decode(encoded)
	print(mapped)
	data_final = decomp.unpredict(decoded)
	print(data_final)
	



#local_sums_visualize(data_one)
#diff_vector_visualize(data_one)
#weight_vector_visualize()
#prediction_calculation_visualize(data_one)
#weight_vector_update_visualize(data_one)
#mapper_visualize(data_one)
#encoder_visualize(data_one)
#decoder_visualize(data_one)
unpredict_visualize(data_one)

#weight_vector_update_visualize(data_two)
