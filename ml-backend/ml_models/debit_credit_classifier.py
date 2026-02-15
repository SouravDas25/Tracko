import pathlib
import pickle

import nltk
import numpy as np
import pandas as pd
from nltk.corpus import stopwords
from sklearn import preprocessing
from sklearn.base import ClassifierMixin
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import accuracy_score
from sklearn.ensemble.gradient_boosting import GradientBoostingClassifier
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import GaussianNB
from sklearn.metrics import confusion_matrix
import matplotlib.pyplot as plt
from sklearn.utils.multiclass import unique_labels
from xgboost.sklearn import XGBClassifier


# import xgboost as xgb


def load():
    try:
        with open(DClassifier.model_filename, 'rb') as file:
            DClassifier.const_model = pickle.load(file)
    except FileNotFoundError:
        DClassifier.const_model = {
            "classifier": XGBClassifier(),
            "vectorized": TfidfVectorizer(ngram_range=(1, 2)),
            "label_encoder": preprocessing.LabelEncoder(),
        }


def clean_sentence(sentence):
    stop_words = stopwords.words('english')
    words = nltk.word_tokenize(sentence.lower())
    words = [w for w in words if w not in stop_words]
    tagged = nltk.pos_tag(words)
    words = [tag[0] for tag in tagged if not tag[1].startswith("NN")]
    return " ".join(words)


class DClassifier(object):
    model_filename = pathlib.Path(__file__).parent / 'dc-model.mdl'
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
        cleaned_sentence = list(map(clean_sentence, dataset))
        t = self.label_encoder.fit_transform(target)
        self.vectorized.fit(cleaned_sentence)
        transformed = self.transform_data(cleaned_sentence)
        # print(t)
        # print(transformed)
        self.classifier.fit(transformed, t)

    def transform_data(self, data):
        return self.vectorized.transform(data).toarray()

    def transform_label(self, target):
        return self.label_encoder.transform(target)

    def predict(self, dataset):
        return self.classifier.predict(dataset)

    def predict_label(self, sentence):
        sentence = clean_sentence(sentence)
        tran = self.transform_data([sentence])
        # print(tran)
        prediction = self.predict(tran)
        # print(prediction)
        return self.label_encoder.inverse_transform(prediction)

    def predit_score(self, dataset, target):
        tran = self.transform_data(dataset)
        # print("tran : ",tran)
        pred = self.predict(tran)
        t = self.transform_label(target)
        print("XGBClassifier accuracy : ", accuracy_score(t, pred, normalize=True))
        cm = pd.crosstab(t, pred, rownames=['True'], colnames=['Predicted'], margins=True)
        print(cm)


if __name__ == "__main__":
    import scratches.load_data as load_data

    dataset, target = load_data.sms_sentences()
    dataset = np.array(dataset)
    # X_train = ['you have won Rs 350 dollars.']
    # y_train = ['NA']
    X_train, X_test, y_train, y_test = train_test_split(dataset, target, test_size=0.33, random_state=42)
    print(dataset)
    print(target)
    model = DClassifier()
    model.fit(X_train, y_train)
    test = "Rs. 500.00 credited to a/c ******0915 on 31-08-18 by a/c linked to VPA  (UPI Ref No  " \
           "824320119972). "
    test = load_data.clean_sentence(test)
    print(test)
    print(model.predict_label(test))
    model.predit_score(X_test, y_test)
    model.save()
    # print(__file__)
