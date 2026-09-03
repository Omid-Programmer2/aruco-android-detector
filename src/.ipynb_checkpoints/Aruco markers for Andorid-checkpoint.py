import cv2
import cv2.aruco as aruco

# این دیکشنری دقیقاً با کد اندروید ما ست شده است
aruco_dict = aruco.getPredefinedDictionary(aruco.DICT_4X4_100)

# تولید مارکر شماره 24 (می‌توانید هر عددی بین 0 تا 99 بگذارید)
marker_id = 24
marker_size = 600 # اندازه به پیکسل

marker_img = aruco.generateImageMarker(aruco_dict, marker_id, marker_size)
cv2.imwrite("marker_24.png", marker_img)

print("فایل marker_24.png ساخته شد. این عکس را در گوشی یا روی کاغذ به برنامه اندروید نشان دهید.")