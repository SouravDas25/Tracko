from sklearn.feature_extraction.text import CountVectorizer, TfidfVectorizer
from sklearn.naive_bayes import GaussianNB
from sklearn.metrics import accuracy_score
from sklearn.model_selection import train_test_split

arr = ["Car was cleaned by Jack",
       "Jack was cleaned by Car."]
arr1 = [
    "Debited Rs 200 from a/c **9502 on 21-6-2013",
    "Paid Rs 200 from a/c **9502 on 21-6-2013.",
    "Rs.3000 deposited in HDFC account on 16-8-2019."
]

target = [
    0, 0, 1
]


#
# def count_vectorize(dataset):
# 	# If you want to take into account just term frequencies:
# 	vectorizer = CountVectorizer(ngram_range=(1, 3))
# 	X = vectorizer.fit_transform(dataset)
# 	# Testing the ngram generation:
# 	print(vectorizer.get_feature_names())
# 	# This will print: ['by car', 'by jack', 'car was', 'cleaned by', 'jack was', 'was cleaned']
# 	print(X.toarray())
# 	return X
#
# def tdidf_vectorize(dataset):
# 	vectorizer = TfidfVectorizer(ngram_range=(2, 2))  # You can still specify n-grams here.
# 	X = vectorizer.fit_transform(dataset)
# 	print(X.toarray())
# 	return X
#
# 	# normalizing
# 	# vectorizer = TfidfVectorizer(ngram_range=(2, 2), norm=None)  # You can still specify n-grams here.
# 	# X = vectorizer.fit_transform(dataset)
# 	# print(X.toarray())
# count_vectorize(arr1)
# X = tdidf_vectorize(arr1).toarray()
# # create an object of the type GaussianNB
# gnb = GaussianNB()
#
# #train the algorithm on training data and predict using the testing data
# pred = gnb.fit(X, target).predict(X)
# #print(pred.tolist())
#
# #print the accuracy score of the model
# print("Naive-Bayes accuracy : ",accuracy_score(target, pred, normalize = True))


class Model(object):

    def __init__(self):
        self.vectorizer = TfidfVectorizer(ngram_range=(1, 3))
        self.classifier = GaussianNB()

    def fit_data(self, dataset, target):
        self.vectorizer.fit(dataset)
        transformed = self.transform(dataset)
        self.classifier.fit(transformed, target)

    def transform(self, data):
        return self.vectorizer.transform(data).toarray()

    def predict(self, dataset):
        return self.classifier.predict(dataset)

    def predit_score(self, dataset, target):
        tran = self.transform(dataset)
        # print("tran : ",tran)
        pred = self.predict(tran)
        print("GaussianNB accuracy : ", accuracy_score(target, pred, normalize=True))


model = Model()
model.fit_data(arr1, target)
model.predit_score(
    ["Rs 144.00 debited from a/c **0915 on 14-03-19 to VPA upiswiggy@icici(UPI Ref No 907310835146). Not you? Call on "
     "18002586161 to report"], [0])
