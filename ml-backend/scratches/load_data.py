import pathlib
import re as regular_expression
import string
import numpy as np
import nltk
import pandas as pd
from nltk import word_tokenize
from nltk.corpus import stopwords

lemma = nltk.stem.WordNetLemmatizer()
porter = nltk.PorterStemmer()


def clean_sentence(sentence):
    stop_words = stopwords.words('english')
    words = nltk.word_tokenize(sentence.lower())
    words = [w for w in words if w not in stop_words]
    tagged = nltk.pos_tag(words)
    words = [tag[0] for tag in tagged if not tag[1].startswith("NN")]
    return " ".join(words)


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
                l.append(porter.stem(w))
            sentences.append(l)
    return sentences


def sms_sentences(filename=None):
    if filename is None:
        filename = pathlib.Path(__file__).parent / 'merge-data.csv'
    global_sentences = []
    targets = []
    excel_dataframe = pd.read_csv(filename)
    # excel_dataframe = excel_dataframe.head(500)
    for i in excel_dataframe.index:
        body = excel_dataframe['body'][i]
        cls = excel_dataframe['class'][i]
        if cls == 'DEBIT' or cls == 'CREDIT':
            global_sentences.append(body.strip().lower())
            targets.append(cls)
        else:
            global_sentences.append(body.strip().lower())
            targets.append("NA")
    global_sentences = list(map(clean_sentence, global_sentences))
    return global_sentences, targets


if __name__ == "__main__":
    s, t = sms_sentences()
    print(s, t)
