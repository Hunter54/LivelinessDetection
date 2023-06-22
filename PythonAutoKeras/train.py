import pandas as pd
import numpy as np
from keras.utils.np_utils import to_categorical
from sklearn.model_selection import train_test_split
import autokeras as ak
import tensorflow as tf

dataframe = pd.read_csv("./fer2013.csv")
print(dataframe)
width = 48
height = 48
pixels = dataframe['pixels'].tolist()
X = []
for xseq in pixels:
    xx = [int(xp) for xp in xseq.split(' ')]
    xx = np.asarray(xx).reshape(width, height)
    X.append(xx.astype('float32'))
X = np.asarray(X)
X = X / 255

y = dataframe['emotion']
y = to_categorical(y, num_classes=7)

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)

classifier = ak.ImageClassifier(overwrite=True, max_trials=2)
classifier.fit(X_train, y_train, epochs=30)
model = classifier.export_model()
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
print(model.summary())
model.save("model_autokeras.h5")
print(classifier.evaluate(X_test, y_test))
