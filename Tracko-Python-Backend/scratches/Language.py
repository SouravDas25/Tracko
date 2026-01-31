# import datefinder
import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize

from scratches.utility import *
from scratches.word_embedding import Embed

stops = set(stopwords.words("english"))

lemma = nltk.stem.WordNetLemmatizer()
stemmer = nltk.SnowballStemmer('english')

"""
    http://logo.clearbit.com/paytm.com

"""


class Language(object):

    def __init__(self):
        pass

    def isValid(self):
        pass

    """
        @param text str
    """

    def process(self, text):
        self.text = text
        self.words = word_tokenize(text.strip().lower())
        self.meaningful_words = [lemma.lemmatize(w) for w in self.words if not w in stops]
        Log.info(self.meaningful_words)
        self.tags = nltk.pos_tag(self.meaningful_words)
        self.chunk = nltk.ne_chunk(self.tags)
        self.dates = datefinder.find_dates(text)
        # self.spacy_nlp = nlp(text)
        self.embeddings = Embed()
        return self

    def getDict(self):
        d = {}
        valid = True
        d['amounts'] = scanAmount(self.tags)
        d['type'] = scanTransactionType(self.text)
        d['dates'] = scanDates(self.dates)
        d['comments'] = scanComments(self.tags)
        if len(d['amounts']) <= 0:
            valid = False
        if d['type'] is None:
            valid = False
        d['valid'] = valid

        return d

    def print_data(self):
        print("words : ", self.words)
        print("meaningful words : ", self.meaningful_words)
        print("tags : ", self.tags)
        print("Entities : ", nltk.tree2conlltags(self.chunk))


# print(word_correlation('debit','deduct'))
lang = Language()
print(lang.process(
    "Rs 144.00 debited from a/c **0915 on 14-03-19 to VPA upiswiggy@icici(UPI Ref No 907310835146). Not you? Call on "
    "18002586161 to report").getDict())
# print_dates(lang.dates)
lang.print_data()
# r = lang.embeddings.model().most_similar("paid")
# print(r)
