import nltk
from nltk.corpus import stopwords
from nltk.corpus import wordnet
import datefinder
import datetime
from nltk.tokenize import word_tokenize
from apis.models import Entity
# import spacy
import re
from gensim.models import Word2Vec

lemma = nltk.stem.WordNetLemmatizer()

# nlp = spacy.load("en_core_web_sm")

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
        nxt = tag[i + 1] if i + 1 < len(tag) else ""
        if chunk[0].lower().startswith("rs") and nxt[1] != "CD":
            match = re.match(r"([a-z]+).?([0-9]+)", chunk[0], re.I)
            Log.info(match)
            if match and len(match.groups()) > 1:
                amt = match.groups()[1]
                amounts.append(float(amt))
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
    max_debit_corr = 0.0
    max_credit_corr = 0.0
    for verb in verbs:
        debit_cor = maxCorrelation(verb, True)
        credit_cor = maxCorrelation(verb, False)
        if debit_cor > max_debit_corr:
            max_debit_corr = debit_cor
        if credit_cor > max_credit_corr:
            max_credit_corr = credit_cor
    if max_debit_corr > 0 and max_credit_corr > 0:
        if max_debit_corr > max_credit_corr:
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
        if tag[1].startswith("NNP") and tag[0].lower() != "rs":
            comments.append(tag[0])
    return comments


def getCategory(properNoun):
    entities = Entity.objects.filter(name__contains=properNoun)
    if len(entities) > 0:
        return entities[0]
    return None


def scanCategry(tags):
    entities = []
    for tag in tags:
        entity = getCategory(tag[0])
        if entity is not None:
            entities.append(entity.get_dict())
    return entities


def printDates(dates):
    for i in dates:
        print(i)


class Log(object):

    @staticmethod
    def info(text):
        print(text)


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

    def __init__(self, text):
        self.text = text
        self.words = word_tokenize(text.strip())
        Log.info(self.words)
        stops = set(stopwords.words("english"))
        self.meaningful_words = [lemma.lemmatize(w) for w in self.words if w not in stops]
        Log.info(self.meaningful_words)
        self.tags = nltk.pos_tag(self.meaningful_words)
        Log.info(self.tags)
        self.dates = datefinder.find_dates(text)
        Log.info(self.dates)

    def getDict(self):
        d = {}
        valid = True
        d['amounts'] = scanAmount(self.tags)
        d['type'] = scanTransactionType(self.tags)
        # d['dates'] = scanDates(self.dates)
        d['comments'] = scanComments(self.tags)
        d['entity'] = scanCategry(self.tags)
        if len(d['amounts']) <= 0:
            valid = False
        if d['type'] is None:
            valid = False
        d['valid'] = valid

        return d


# print(word_correlation('debit','deduct'))
# lang = Language(" I have paid Mr. Arvani Rs 200 on 12-02-19.")
# print(lang.getDict())
# printDates(lang.dates)
