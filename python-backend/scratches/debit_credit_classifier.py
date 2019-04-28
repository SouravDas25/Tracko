import pickle
import numpy as np
from sklearn import preprocessing
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import accuracy_score
from sklearn.naive_bayes import GaussianNB
from sklearn.model_selection import train_test_split

import load_data


def load():
    try:
        with open(DClassifier.model_filename, 'rb') as file:
            DClassifier.const_model = pickle.load(file)
    except FileNotFoundError:
        DClassifier.const_model = {
            "classifier": GaussianNB(),
            "vectorized": TfidfVectorizer(ngram_range=(1, 3)),
            "label_encoder": preprocessing.LabelEncoder(),
        }


class DClassifier(object):
    model_filename = 'dc-model.mdl'
    const_model = None

    def __init__(self):
        if DClassifier.const_model is None:
            load()
        obj = DClassifier.const_model
        self.vectorized = obj['vectorized']
        self.label_encoder = obj['label_encoder']
        self.classifier = obj['classifier']

    def save(self):
        obj = {
            "classifier": self.classifier,
            "vectorized": self.vectorized,
            "label_encoder": self.label_encoder,
        }
        with open(DClassifier.model_filename, 'wb') as file:
            pickle.dump(obj, file)

    def fit(self, dataset, target):
        t = self.label_encoder.fit_transform(target)
        self.vectorized.fit(dataset)
        transformed = self.transform_data(dataset)
        # print(t)
        # print(transformed)
        self.classifier.fit(transformed, t)

    def transform_data(self, data):
        return self.vectorized.transform(data).toarray()

    def transform_label(self, target):
        return self.label_encoder.transform(target)

    def predict(self, dataset):
        return self.classifier.predict(dataset)

    def predict_label(self, data):
        tran = self.transform_data([data])
        # print(tran)
        prediction = self.predict(tran)
        # print(prediction)
        return self.label_encoder.inverse_transform(prediction)

    def predit_score(self, dataset, target):
        tran = self.transform_data(dataset)
        # print("tran : ",tran)
        pred = self.predict(tran)
        t = self.transform_label(target)
        print("GaussianNB accuracy : ", accuracy_score(t, pred, normalize=True))


if __name__ == "__main__":
    dataset, target = load_data.sms_sentences()
    dataset = np.array(dataset)
    X_train, X_test, y_train, y_test = train_test_split(dataset, target, test_size=0.33, random_state=42)
    print(dataset)
    print(target)
    model = DClassifier()
    # model.fit(X_train, y_train)
    test = "paid rs 30 to sourav."
    print(model.predict_label(test))
    model.predit_score(X_test, y_test)
    # model.save()
