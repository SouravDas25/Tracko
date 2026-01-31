from pprint import pprint

import nltk
from nltk.corpus import stopwords
from nltk.stem.porter import PorterStemmer
import re as regular_expression
import string

porter = PorterStemmer()


def split_sentence(sentence):
    stop_words = stopwords.words('english')
    words = nltk.word_tokenize(sentence.lower())
    words = [w for w in words if w not in stop_words]
    l = []
    for i in range(len(words)):
        if words[i] == '.' and not words[i - 1].isnumeric():
            continue
        if words[i] not in '.{}' and words[i] in string.punctuation:
            continue
        if regular_expression.search(r"[a-zA-Z$]+\.[0-9.]", words[i]) is not None:
            spl = words[i].split(".")
            l.append(spl[0])
            l.append(".".join(spl[1:]))
            continue
        l.append(words[i])
    words = l
    words = [porter.stem(word) for word in words]
    # words = [w.lower() for w in words if w.isalnum()]
    return words


def get_sentences_from_file(filename: str) -> list:
    lines = []
    sentence = ""
    with open(filename, 'r') as file:
        line = " "
        while len(line) > 0:
            line = file.readline()

            if line.strip() == "#data":
                sentence = ""
                continue

            if line.strip() == "#end-data":
                lines.append(sentence)
                sentence = ""
                # continue

            sentence += line.strip()

            # print(line)
    lines = clean_sentences(lines)
    # pprint(lines)
    return lines


def clean_sentence(sentence: str) -> str:
    sentence = regular_expression.sub(r"[,]+", "", sentence)
    sentence = regular_expression.sub(r"[!\"#$%&'()*+/;<=>:?[\]^_`~]+", " ", sentence)
    return sentence


def clean_sentences(sentences: list) -> list:
    return [clean_sentence(sentence) for sentence in sentences]
