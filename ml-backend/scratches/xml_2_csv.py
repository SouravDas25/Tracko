import pandas as pd
import xml.etree.ElementTree as ET

date = []
body = []
address = []


def xml_to_csv(xmlfile):
    tree = ET.parse(xmlfile)
    root = tree.getroot()
    for item in root.findall('.sms'):
        address.append(item.attrib["address"])
        body.append(item.attrib["body"])
        date.append(item.attrib["date"])

    df = pd.DataFrame()
    df['address'] = address
    df['date'] = date
    df['body'] = body
    # print(df)
    df.to_csv(xmlfile+ ".csv")


if __name__ == "__main__":
    xml_to_csv("sms-data-aman")
