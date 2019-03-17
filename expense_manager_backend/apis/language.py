import nltk
from nltk.corpus import stopwords
from nltk.corpus import wordnet
import datefinder
import datetime

lemma = nltk.stem.WordNetLemmatizer()


# print(tags)

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


def scanTransactionType(tags):
    verbs = []
    for i in range(len(tags)):
        pos = tags[i]
        if pos[1].startswith("VB"):
            verbs.append(pos[0])
    maxDebitCorr = 0.0
    maxCreditCorr = 0.0
    for verb in verbs:
        debitCor = maxCorrelation(verb, True)
        creditCor = maxCorrelation(verb, False)
        if debitCor > maxDebitCorr:
            maxDebitCorr = debitCor
        if creditCor > maxCreditCorr:
            maxCreditCorr = creditCor
    if maxDebitCorr > 0 and maxCreditCorr > 0:
        if maxDebitCorr > maxCreditCorr:
            return "Debit"
        else:
            return "Credit"
    return None


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


class Log(object):

    @staticmethod
    def info(text):
        print(text);

"""
clearbit
souravbumbadas25@gmail.com
SD2525SD25
pk_bc92554fd02164456be28099e35a2fa4
"""

class Language(object):

    def isValid(self):
        pass

    """
        @param text str
    """

    def process(self, text):
        self.text = text
        self.words = text.strip().split(" ")
        stops = set(stopwords.words("english"))
        self.meaningful_words = [lemma.lemmatize(w) for w in self.words if not w in stops]
        Log.info(self.meaningful_words)
        self.tags = nltk.pos_tag(self.meaningful_words)
        self.dates = datefinder.find_dates(text)
        return self

    def getDict(self):
        d = {}
        valid = True
        d['amounts'] = scanAmount(self.tags)
        d['type'] = scanTransactionType(self.tags)
        d['dates'] = scanDates(self.dates)
        d['comments'] = scanComments(self.tags)
        if len(d['amounts']) <= 0:
            valid = False
        if d['type'] == None:
            valid = False
        d['valid'] = valid