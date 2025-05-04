# 团队日志

---
### 2025.4.10 fjy

establish the project and the basic structure of the code.

### 2025.4.16 fjy

add some course files and pdf

### 2025.4.16 fjy

The first step:

1.transform the picture into graph

2.calculate the Ix, Iy, G and normalize the f_G

3.build Graph and calculate the cost

---
### 2025.4.17 fjy

The second step:

1.work out the simple GUI to visualize the picture and the outcome

2.because the picture is compressed when visualizing, so the computeShortestPath has some problems. However if you use the original picture to computeShortestPath, it works well.

3.To do:

a.figure the compressed problem

b.understand the code

c.make it can continuously select the borders.

### 2025.4.17 fjy

I'm completed drop a file into the application and worked out the code to continuously select the borders.

To do:

a.when run the application, it shows that "please drop a ...."

b.compressed problem

c.make the application stick on the top to operate.

### 2025.4.21 zyt

try to solve the problem of scaled image.--GUI2

(separate processing of two types of image) 

### 2025.4.22 xbx
1. solve problem of scaled image completely based on GUI
try to do works according to gif
2. add ending condition: double click
3. after double clicks, point path between seed and last seed and use save button. But one problem, use dijkstra or straight line directly. Gif using dijkstra, but when we use it, the path not very well. I prefer the second one.
4. add boolean isDragging: if double clicks, forbid MouthMove
5. try to modify saveButton, save path and save the image
6. optimize dijkstra slightly

need to do:
1. output saved image
2. optimize load and compute
3. optimize edge
4. GUI

### 2025.4.25 zyt
1. add drag prompts
2. add Cursor Snap, capable of automatically identifying the best in the field

To do:
1. optimize load and compute
2. optimize edge
3. path cooling
4. GUI
5. save gif

### 2025.4.25 fjy
1. fix the problem of leaving the gui always on top
2. show the output image

To do:
1. the output visualization's scale need to be the same as the gui
2. the gui is setAlways on top, I don't know if it's ok
3. the cusor snap is changed, now it has obvious effect, but the red curve is divided, it depends on the point of the mouse, rather than the seeds

### 2025.4.29 xbx
1. optimize computing the shortest by using multithreading
2. try to use LWJGL to load image

To do:
1. debug of LWJGL(have some problems)
2. optimize compute by using LWJGL(speed up by using GPU? Memory access, change data structure) and cache
3. optimize edge
4. Real-time scale:solution to 'the gui is setAlways on top, I don't know if it's ok'
5. the output visualization's scale need to be the same as the gui
6. save gif
7. the cusor snap is changed, now it has obvious effect, but the red curve is divided, it depends on the point of the mouse, rather than the seeds
8. path cooling

### 2025.4.30 xbx
1. use LWJGL to load image successfully(I will do LWJGL and origin both. Up to ddl), the speed of loading image by LWJGL is very fast, the time can be ignored 
2. simply revise the outputImage
3. use gaussianBlur and replace sobel by scharr and enhance post-edge-detection to optimize edge

To do:
1. further try LWJGL
2. optimize compute by using LWJGL(speed up by using GPU? Memory access, change data structure) and cache
3. Real-time scale:solution to 'the gui is setAlways on top, I don't know if it's ok'
4. the output visualization's scale need to be the same as the gui
5. save gif: need maven proj
6. the cusor snap is changed, now it has obvious effect, but the red curve is divided, it depends on the point of the mouse, rather than the seeds
7. optimize path cooling by 动态阈值调整 路径方向预测 性能优化