from nltk.corpus import wordnet
import datetime

# import spacy

# nlp = spacy.load("en_core_web_lg")


# print(tags)
from scratches.debit_credit_classifier import DClassifier


def word_correlation(word1, word2):
    wordFromList1 = wordnet.synsets(word1, pos=wordnet.VERB)
    wordFromList2 = wordnet.synsets(word2, pos=wordnet.VERB)
    # print(wordFromList1,wordFromList2)
    if wordFromList1 and wordFromList2:  # Thanks to @alexis' note
        s = wordFromList1[0].wup_similarity(wordFromList2[0])
        return s
    return 0.0


def scanAmount(tag):
    amounts = []
    for i in range(len(tag)):
        chunk = tag[i]
        prev = tag[i - 1][0].lower() if i - 1 >= 0 else ""
        # print(chunks[i+1][0])
        nxt = tag[i + 1][0].lower() if i + 1 < len(tag) else ""
        if chunk[1] == "CD" and "rs" in (prev, nxt):
            amounts.append(float(chunk[0]))
    return amounts


creditList = ["credit", "deposit", "increment"]
debitList = ["debit", "paid", "withdraw", "deduct", "decrement"]


def maxCorrelation(verb, CD):
    max = 0
    list = debitList if CD else creditList
    for i in list:
        correlation = word_correlation(verb, i)
        if correlation > max:
            max = correlation
    return max


def scanTransactionType(sentence):
    # verbs = []
    # for i in range(len(tags)):
    #     pos = tags[i]
    #     if pos[1].startswith("VB"):
    #         verbs.append(pos[0])
    # maxDebitCorr = 0.0
    # maxCreditCorr = 0.0
    # for verb in verbs:
    #     debitCor = maxCorrelation(verb, True)
    #     creditCor = maxCorrelation(verb, False)
    #     if debitCor > maxDebitCorr:
    #         maxDebitCorr = debitCor
    #     if creditCor > maxCreditCorr:
    #         maxCreditCorr = creditCor
    # if maxDebitCorr > 0 and maxCreditCorr > 0:
    #     if maxDebitCorr > maxCreditCorr:
    #         return "Debit"
    #     else:
    #         return "Credit"
    # return None
    model = DClassifier()
    prediction = model.predict_label(sentence)
    if prediction == 'NA':
        return None
    return prediction[0]


def scanDates(dates):
    d = []
    now = datetime.datetime.now()
    lowDate = now - datetime.timedelta(days=120)
    for i in dates:
        if lowDate <= i <= now:
            d.append(i)
    return d


"""
    @param tags 
"""


def scanComments(tags):
    comments = []
    for tag in tags:
        if tag[1].startswith("NN") and tag[0].lower() != "rs":
            comments.append(tag[0])
    return comments


def printDates(dates):
    for i in dates:
        print(i)


class Log(object):

    @staticmethod
    def info(text):
        print(text)
