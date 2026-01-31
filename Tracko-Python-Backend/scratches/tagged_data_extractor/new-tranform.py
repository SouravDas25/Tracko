import pandas as pd
import nltk
from sklearn.feature_extraction import DictVectorizer
import numpy as np
from sklearn.linear_model import Perceptron
from sklearn.metrics import classification_report, accuracy_score
from sklearn.model_selection import train_test_split


def generate_csv(sentences):
    dic = {'sentence': [], 'words': [], 'pos': [], 'tags': []}
    for sen in sentences:
        dic['sentence'].append(sen)
        words = nltk.word_tokenize(sen)
        pos_tag = nltk.pos_tag(words)
        for word, pos in pos_tag:
            dic['words'].append(word)
            dic['pos'].append(pos)
        dic['sentence'].extend([None] * (len(words) - 1))
        dic['tags'].extend([None] * len(words))
    df = pd.DataFrame(dic)
    df.to_csv("data.csv", index=False)


if __name__ == '__main__':
    # dataframe = pd.read_excel("D:\\Programming\\Tracko-Python-Backend\\scratches\\sms-data.xls")
    # data = list(dataframe[dataframe['class'] == 'DEBIT'].head()['Body'])
    # generate_csv(data)
    df = pd.read_csv('data.csv')
    print(df)
    df = df.fillna(value="")

    X = df.drop(columns=['tags', 'sentences'], axis=1)
    v = DictVectorizer(sparse=False)
    X = v.fit_transform(X.to_dict('records'))
    y = df.tags.values

    classes = np.unique(y)
    classes = classes.tolist()

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.33, random_state=0)
    print(X_train.shape, y_train.shape)

    per = Perceptron(verbose=10, n_jobs=-1, max_iter=5)
    per.partial_fit(X_train, y_train, classes)

    new_classes = classes.copy()
    new_classes.pop()

    print(classification_report(y_pred=per.predict(X_test), y_true=y_test, labels=new_classes))
    print(accuracy_score(y_test, per.predict(X_test)))
