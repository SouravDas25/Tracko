# import datefinder
import datetime
# import spacy
import re

import nltk
from nltk.corpus import stopwords
from nltk.corpus import wordnet
from nltk.tokenize import word_tokenize

from apis.models import Entity, Tag
from ml_models.debit_credit_classifier import DClassifier

lemma = nltk.stem.WordNetLemmatizer()


# nlp = spacy.load("en_core_web_sm")

# print(tags)

def word_correlation(word1, word2):
    word_from_list1 = wordnet.synsets(word1, pos=wordnet.VERB)
    word_from_list2 = wordnet.synsets(word2, pos=wordnet.VERB)
    # print(word_from_list1,word_from_list2)
    if word_from_list1 and word_from_list2:  # Thanks to @alexis' note
        s = word_from_list1[0].wup_similarity(word_from_list2[0])
        return s
    return 0.0


def is_currency_symbol(word: str, before: bool) -> bool:
    if word is None:
        return False
    word = word.lower()
    if before:
        currency = ['rs', 'inr', '$']
        for i in currency:
            if word == i:
                return True
    else:
        after_currency = ['$']
        for i in after_currency:
            if word == i:
                return True
    return False


def scan_amount(tag):
    amounts = []
    for i in range(len(tag)):
        chunk = tag[i]
        prev = tag[i - 1][0].lower() if i - 1 >= 0 else ""
        prev2 = tag[i - 2][0].lower() if i - 2 >= 0 else ""
        # print(chunks[i+1][0])
        nxt = tag[i + 1][0].lower() if i + 1 < len(tag) else ""
        is_currency_symbol_present = is_currency_symbol(prev, True) or is_currency_symbol(prev2, True) \
                                     or is_currency_symbol(nxt, False)
        if chunk[1] == "CD" and is_currency_symbol_present:
            amounts.append(float(chunk[0]))
        nxt = tag[i + 1] if i + 1 < len(tag) else ""
        if chunk[0].lower().startswith("rs") and nxt[1] != "CD":
            match = re.match(r"([a-z]+).?([0-9]+)", chunk[0], re.I)
            Log.info(match)
            if match and len(match.groups()) > 1:
                amt = match.groups()[1]
                amounts.append(float(amt))
    return [amt for amt in amounts if amt < 999999]


creditList = ["credit", "deposit", "increment"]
debitList = ["debit", "paid", "withdraw", "deduct", "decrement"]


def max_correlation(verb, CD):
    max = 0
    list = debitList if CD else creditList
    for i in list:
        correlation = word_correlation(verb, i)
        if correlation > max:
            max = correlation
    return max


def scan_transaction_type(sentence):
    # verbs = []
    # for i in range(len(tags)):
    #     pos = tags[i]
    #     if pos[1].startswith("VB"):
    #         verbs.append(pos[0])
    # max_debit_corr = 0.0
    # max_credit_corr = 0.0
    # for verb in verbs:
    #     debit_cor = max_correlation(verb, True)
    #     credit_cor = max_correlation(verb, False)
    #     if debit_cor > max_debit_corr:
    #         max_debit_corr = debit_cor
    #     if credit_cor > max_credit_corr:
    #         max_credit_corr = credit_cor
    # if max_debit_corr > 0 and max_credit_corr > 0:
    #     if max_debit_corr > max_credit_corr:
    #         return "Debit"
    #     else:
    #         return "Credit"
    # return None
    model = DClassifier()
    prediction = model.predict_label(sentence)[0]
    if prediction == 'NA':
        return None
    return prediction


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


def scan_comments(tags):
    comments = []
    for tag in tags:
        if tag[1].startswith("NNP") and tag[0].lower() != "rs":
            comments.append(tag[0])
    return comments


def getCategory(properNoun):
    entities = Entity.objects.all()
    for e in entities:
        p = properNoun.lower()
        n = e.name.lower()
        if p.startswith(n) or p.endswith(n):
            return e
    # if len(entities) > 0:
    # return entities[0]
    return None


def getTag(properNoun):
    tags = Tag.objects.all()
    for e in tags:
        p = properNoun.lower()
        n = e.name.lower()
        if p.startswith(n) or p.endswith(n):
            return e.entity
    # if len(entities) > 0:
    # return entities[0]
    return None


def scanCategry(tags, address):
    entities = []
    a = []
    a.extend(tags)
    a.extend(address)
    for tag in a:
        entity = getCategory(tag[0])
        if entity is not None:
            entities.append(entity.get_dict())
        entity = getTag(tag[0])
        if entity is not None:
            entities.append(entity.get_dict())

    # for tag in address:
    #     entities.extend(getCategory(tag))
    #     entities.extend(scanInTags(tag))

    return entities


def print_dates(dates):
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

    def is_valid(self):
        pass

    """
        @param text str
    """

    def __init__(self, text, address):
        self.text = text
        self.address = word_tokenize(address.strip())
        self.address = nltk.pos_tag(self.address)
        self.words = word_tokenize(text.strip())
        Log.info(self.words)
        stops = set(stopwords.words("english"))
        self.meaningful_words = [w for w in self.words if w not in stops]
        Log.info(self.meaningful_words)
        self.tags = nltk.pos_tag(self.meaningful_words)
        Log.info(self.tags)
        # self.dates = datefinder.find_dates(text)
        # Log.info(self.dates)

    def get_dict(self):
        d = {}
        valid = True
        d['amounts'] = scan_amount(self.tags)
        d['type'] = scan_transaction_type(self.text)
        # d['dates'] = scanDates(self.dates)

        d['comments'] = scan_comments(self.tags)
        d['entity'] = scanCategry(self.tags, self.address)
        # d['entity'].extend(scanCategry(self.address))

        if len(d['amounts']) <= 0:
            valid = False
        if d['type'] is None:
            valid = False
        d['valid'] = valid
        d['nlp'] = {
            "address": self.address,
            "words": self.words,
            "meaningful_words": self.meaningful_words,
            "tags": self.tags,
        }

        return d


if __name__ == "__main__":
    print(word_correlation('debit', 'deduct'))
    lang = Language(" I have paid Mr. Arvani Rs 200 on 12-02-19.")
    print(lang.get_dict())
    print_dates(lang.dates)
