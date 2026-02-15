import re

import nltk
import numpy as np
import pandas as pd
from nltk.corpus import stopwords
from nltk.stem.porter import PorterStemmer
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.feature_extraction import DictVectorizer
from sklearn.metrics import classification_report, accuracy_score
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import GaussianNB
from sklearn.neural_network import MLPClassifier
from sklearn.preprocessing import LabelEncoder, StandardScaler, OneHotEncoder
from extract_sentences import *
from OpenAi.sa import encoder
from sklearn.tree import DecisionTreeClassifier

porter = PorterStemmer()

MAX_WORD_IN_FEED = 1


# x = re.search(r"\{([^\|\}\{]*)\|([^\|\{\{]*)\}", i)
def extract_annotations(sentences: list) -> list:
    matches = []
    for sentence in sentences:
        words = split_sentence(sentence)
        entities = extract_annotations_from_words(words)
        matches.append(entities)
    return matches


def split_word_classification(annotation: str, entity: dict) -> str:
    word, classification = annotation.split("|")
    word = split_sentence(word)
    entity['len'] = len(word)
    entity['class'] = classification.strip()
    return word


def extract_annotations_from_words(words: list) -> dict:
    data = {}
    entities = []
    start = False
    annotation = None
    entity = {}
    original_words = []
    for i in range(len(words)):
        current_word = words[i]
        if current_word == '{':
            entity = {'start': len(original_words)}
            start = True
            annotation = ""
            continue
        if start and current_word == '}':
            word = split_word_classification(annotation, entity)
            entities.append(entity)
            original_words.extend(word)
            start = False
            continue

        if start:
            # if words[i - 1] != '{':
            #     annotation += "-"
            annotation += current_word + " "
            continue

        original_words.append(current_word)
    data['original'] = " ".join(original_words)
    data['entities'] = entities
    return data


def val_or_null(index: int, iterable: list) -> object:
    if index <= 0 or index >= len(iterable):
        return None
    return iterable[index]


def generate_feed_data(sentence: str) -> list:
    sentence_feed = []
    words = split_sentence(sentence.lower())
    pos = nltk.pos_tag(words)
    for index in range(len(words)):
        feed_data = dict()
        feed_data['word'] = pos[index][0]
        feed_data['pos'] = pos[index][1]
        for i in range(1, MAX_WORD_IN_FEED + 1):
            pos1 = val_or_null(index - i, pos)
            pos2 = val_or_null(index + i, pos)
            # feed_data['word-' + str(i)] = pos1[0] if pos1 is not None else None
            # feed_data['word+' + str(i)] = pos2[0] if pos2 is not None else None
            # feed_data['pos-' + str(i)] = pos1[1] if pos1 is not None else None
            # feed_data['pos+' + str(i)] = pos2[1] if pos2 is not None else None
        sentence_feed.append(feed_data)
    return sentence_feed


def generate_dataset(annotated_sentences: list) -> tuple:
    dataset = []
    classifications = []
    index0 = 0
    for sentence_annotation in annotated_sentences:
        feed_sentence = generate_feed_data(sentence_annotation['original'])
        for feed_data in feed_sentence:
            classifications.append('NA')
        # print(pos)
        dataset.extend(feed_sentence)
        for entities in sentence_annotation['entities']:
            index = entities['start'] + index0
            classifications[index] = entities['class']
            # print(feed_datas)
            # classifications.append(entities['class'])
            # dataset.extend(words)
        index0 = len(classifications)
    return dataset, classifications


class DataSource(object):

    @staticmethod
    def with_annotations(data_input, isFile=True):
        self = DataSource()
        if isFile:
            self.sentences = get_sentences_from_file(data_input)
        else:
            self.sentences = data_input
        self.annotated_sentences = extract_annotations(self.sentences)
        self.originals = list(map(lambda x: x['original'], self.annotated_sentences))
        self.dataset, self.target = generate_dataset(self.annotated_sentences)
        return self

    @staticmethod
    def without_annotations(data_input, isFile=False):
        self = DataSource()
        if isFile:
            self.sentences = get_sentences_from_file(data_input)
        else:
            self.sentences = data_input
        self.dataset = generate_feed_data(self.sentences)
        return self

    def __init__(self):
        self.dataset = None
        self.target = None


def write_annotated_sentences(sentences, filename):
    with open(filename, 'w') as file:
        for sentence in sentences:
            file.write("#data\n")
            file.write(sentence + "\n")
            file.write("#end-data\n\n")
    return True


def getPosList():
    return ['CC', 'CD', 'DT', 'EX', 'FW', 'IN', 'JJ', 'JJR', 'JJS', 'LS', 'MD', 'NN', 'NNS', 'NNP', 'NNPS', 'PDT',
            'POS', 'PRP', 'PRP$', 'RB', 'RBR', 'RBS', 'RP', 'SYM', 'TO', 'UH', 'VB', 'VBD', 'VBG', 'VBN', 'VBP', 'VBZ',
            'WDT', 'WP', 'WP$', 'WRB', '.', ',', ';', '(', ')', ':', '#', "None"]


pos_label_encoder = LabelEncoder()
ohe = pos_label_encoder.fit_transform(getPosList())
pos_one_hot_encoding = OneHotEncoder(sparse=False)
pos_one_hot_encoding.fit(ohe.reshape(-1, 1))
word_encoder = encoder.Model()


def encode_pos(pos):
    l = pos_label_encoder.transform([str(pos)])
    return pos_one_hot_encoding.transform(l.reshape(-1, 1))[0]


class NerModel(object):

    def __init__(self):
        self.model = GradientBoostingClassifier()
        # self.model = MLPClassifier(alpha=1, max_iter=1000)
        self.vec = DictVectorizer(sparse=False)
        self.target_encoder = LabelEncoder()

    def transform_input_vector(self, feed_sentence):
        d = self.encode_word_embeddings(feed_sentence)
        # d = self.vec.transform(feed_sentence)
        d = np.nan_to_num(np.array(d))
        return d

    def transform(self, dataset: list, target: list):
        self.vec.fit(dataset)
        transformed = self.transform_input_vector(dataset)
        self.target_encoder.fit(target)
        target_y = np.array(self.target_encoder.transform(target))
        return transformed, target_y

    def encode_word_embedding(self, word_embedding: dict) -> list:
        """
        :param word_embedding:  {'words': 'paid', 'pos': 'VBN', 'word-1': None, 'word+1': 'rs', 'pos-1': None, 'pos+1': 'NN'}
        :return: list of vectors
        """
        encoding = []
        for key in word_embedding:
            if "pos" in key:
                encoding.extend(encode_pos(word_embedding[key]))
            else:
                features = word_encoder.transform(word_embedding[key]).reshape(1, -1)[0]
                encoding.extend(features)
        print(encoding, word_embedding)
        return encoding

    def encode_word_embeddings(self, word_embeddings: list) -> list:
        return [self.encode_word_embedding(we) for we in word_embeddings]

    def fit(self, x: list, y: list, kls) -> None:
        self.model.fit(x, y)

    def predict(self, encoded):
        return self.model.predict(encoded)

    def generate_annotated_sentences(self, sentences):
        annotated_sentences = []
        for sentence in sentences:
            data_source = DataSource.without_annotations(sentence, isFile=False)
            transformed = self.transform_input_vector(data_source.dataset)
            predictions = self.predict(transformed)
            predictions = self.target_encoder.inverse_transform(predictions)
            new_sentence = ""
            for feed_data, pred in zip(data_source.dataset, predictions):
                if pred == 'NA':
                    new_sentence += feed_data['word'] + " "
                else:
                    new_sentence += "{" + feed_data['word'] + "|" + pred.upper() + "} "
            annotated_sentences.append(new_sentence)
        return annotated_sentences


if __name__ == '__main__':
    # df = pd.read_excel('../sms-data.xls', sheet_name='Sheet1')
    # print(df)
    # model = gensim.models.Doc2Vec(df['Body'])

    training_source = DataSource.with_annotations('sms-data.md')
    for data, tag in zip(training_source.dataset, training_source.target):
        print(data['word'], tag, end=", ")
    print()
    print(np.array(training_source.dataset))
    model = NerModel()

    x, y = model.transform(training_source.dataset[:500], training_source.target[:500])
    scaler = StandardScaler().fit(x)
    x = scaler.transform(x)

    X_train, X_test, y_train, y_test = train_test_split(x, y, test_size=0.33, random_state=0)
    print(X_train.shape, X_test.shape)
    print(X_train)

    classes = np.unique(y)
    classes = classes.tolist()

    model.fit(X_train, y_train, classes)

    new_classes = classes.copy()
    new_classes.pop()

    print(classification_report(y_pred=model.predict(X_test), y_true=y_test, labels=new_classes))
    print(accuracy_score(y_test, model.predict(X_test)))

    dataframe = pd.read_excel("../sms-data.xls")
    data = list(dataframe[dataframe['class'] == 'DEBIT'].head(25)['Body'])

    annotated_sentences = model.generate_annotated_sentences(data)
    print(np.array(annotated_sentences))
    # write_annotated_sentences(annotated_sentences, 'new-data.md')
