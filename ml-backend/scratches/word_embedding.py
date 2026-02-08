import gensim
import nltk
from gensim.models import Word2Vec

import pandas as pd
from nltk import word_tokenize
from nltk.corpus import stopwords
import pickle

lemma = nltk.stem.WordNetLemmatizer()
stemmer = nltk.PorterStemmer()




class Embed(object):

    model_file = 'sms-data.mdl'
    const_model = None

    def __init__(self):
        if Embed.const_model is None:
            with open(Embed.model_file, 'rb') as infile:
                Embed.const_model = pickle.load(infile)
        self.model_w2v = Embed.const_model

    def fit(self, sentences):
        # self.model_w2v = Word2Vec(data, min_count=1, size=100, window=5)
        self.model_w2v.train(sentences)
        return self

    def model(self):
        return self.model_w2v


def load_extract(filename):
    sentences = []
    excel_dataframe = pd.read_excel(filename, sheetname='Sheet1')
    for i in excel_dataframe.index:
        body = excel_dataframe['Body'][i]
        cls = excel_dataframe['class'][i]
        if cls == 'DEBIT' or cls == 'CREDIT':
            wrds = word_tokenize(body.strip().lower())
            l = []
            for w in wrds:
                # l.append(lemma.lemmatize(w))
                # l.append(w)
                l.append(stemmer.stem(w))
            sentences.append(l)
    return sentences
    # print(excel_dataframe)


def save_model(filename, model):
    with open(filename, "wb") as outfile:
        pickle.dump(model, outfile)


def run(filename, ext):
    sentences = load_extract(filename + ext)
    print(sentences)
    model = Word2Vec(sentences, min_count=1, size=100, window=5)
    save_model(filename + '.mdl', model)
    # print(words)


if __name__ == "__main__":
    run('sms-data', '.xls')
