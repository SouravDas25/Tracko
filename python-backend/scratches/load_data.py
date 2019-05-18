import os
import pathlib

import nltk
import pandas as pd
from nltk import word_tokenize

lemma = nltk.stem.WordNetLemmatizer()
stemmer = nltk.PorterStemmer()


def sms_words(filename):
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


def sms_sentences(filename=None):
    if filename is None:
        filename = pathlib.Path(__file__).parent / 'sms-data.xls'
    global_sentences = []
    targets = []
    excel_dataframe = pd.read_excel(filename, sheet_name='Sheet1')
    for i in excel_dataframe.index:
        body = excel_dataframe['Body'][i]
        cls = excel_dataframe['class'][i]
        if cls == 'DEBIT' or cls == 'CREDIT':
            global_sentences.append(body.strip().lower())
            targets.append(cls)
        else:
            global_sentences.append(body.strip().lower())
            targets.append("NA")
    return global_sentences, targets


if __name__ == "__main__":
    s, t = sms_sentences()
    print(s, t)
