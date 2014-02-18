# coding=utf-8
import urllib2, urllib, socket, json, cookielib, os, string
from cStringIO import StringIO
import codecs
from optparse import OptionParser
import sys


global attr_dict 
attr_dict = {}
global nameList 
nameList = ['director', 'actors', 'country', 'type', 'release_year', 'class']

default_encoding = 'utf-8'
if sys.getdefaultencoding() != default_encoding:
    reload(sys)
    sys.setdefaultencoding(default_encoding)

def commandLineArgs():
    parser = OptionParser()
    parser.add_option( '--config-file', dest = 'config_file',help = 'configuration file in json format')
    parser.add_option('--output-file', dest = 'output_file',help = 'output file stores result in json formate')
    parser.add_option('--temp-dir', dest = 'temp_dir', help = 'temporary dir stores temporary files generated in craweling process')
    (options, args) = parser.parse_args()
    return options
                      
def getElementFromJson(json_obj, path):
    current_obj = json_obj
    result = None
    tmps = path.split('->')
    num = len(tmps)
    
    sec_point = 0
    while sec_point < num:
        section = tmps[sec_point]
        if current_obj.get(section):
            if sec_point == (num -1):
                result = current_obj[section]
            else:
                current_obj = current_obj[section]
            sec_point += 1
        else:
            #print "no content in this section: ", section
            break
    
    return result

def readJsonObjFromFile(file_name, path):
    f = open(file_name, "r")
    s = f.read()
    obj = json.loads(s)
    
    if path is None:
        return obj
    else:
        return getElementFromJson(obj, path)

def getConfig():
    config_file = "conf/config.json"
    output_file = "temp/movie.arff"
    temp_dir = "temp"
    
    options = commandLineArgs()
    if options.config_file is not None:
        config_file = str(options.config_file)
    if options.output_file is not None:
        output_file = options.output_file
    if options.temp_dir is not None:
        temp_dir = options.temp_dir
    return config_file, output_file, temp_dir

def init(config_file, temp_dir):
    if not os.path.exists(temp_dir):
        os.makedirs(temp_dir)
    
    for the_file in os.listdir(temp_dir):
        file_path = os.path.join(temp_dir, the_file)
        try:
            if os.path.isfile(file_path):
                os.unlink(file_path)
        except Exception, e:
            print e
    
    timeout = 30
    socket.setdefaulttimeout(timeout)
    cookie_jar = cookielib.LWPCookieJar()
    cookie = urllib2.HTTPCookieProcessor(cookie_jar)
#    proxy_array = readJsonObjFromFile(config_file, "proxy")
    
#    if proxy_array is not None:
#        num = len(proxy_array)
#        if num > 0:
#            proxy_conf = {}
#            for item in proxy_array:
#                proxy_conf[getElementFromJson(item, "protocal")] = getElementFromJson(item,
#                        "hostPort")
#            proxy = urllib2.ProxyHandler(proxy_conf)
#            opener = urllib2.build_opener(proxy, cookie)
#        else:
#            opener = urllib2.build_opener(cookie)
#    else:
#        opener = urllib2.build_opener(cookie)

    opener = urllib2.build_opener(cookie)
    urllib2.install_opener(opener)
    return opener

def sendRequest(opener, url, param, header, outputfile):
    data_encoded = urllib.urlencode(param)
  
    if header is None:
        if param is None:
            req = urllib2.Request(url)
        else:
            req = urllib2.Request(url, data_encoded)
    else:
        req = urllib2.Request(url, data_encoded, header) 

    print "request sent"

    response = opener.open(req)
    the_page = response.read()
    if outputfile is not None:
        open(outputfile, "w").write(the_page)
    return the_page

def parseResponse(response,output,pageNum,temp_dir):
    global nameList
    global attr_dict
    json_obj = json.loads(response)
    subjects = getElementFromJson(json_obj, 'subjects')
    movie_obj = {}
    if subjects is not None:
        num = len(subjects)
        if num > 0:
            for moive in subjects:
                movie_obj.clear()
                movie_obj['title'] = getElementFromJson(moive, 'title')
                abstract = getElementFromJson(moive, 'abstract')
                movie_obj['release_year'] = getElementFromJson(moive, 'release_year')
                url = getElementFromJson(moive, 'url')
                movie_obj['rater'] = getElementFromJson(moive, 'rater')
                movie_obj['rate'] = getElementFromJson(moive, 'rate')

                strlist = url.split('/')
                movie_obj['id'] = strlist[len(strlist) -2]
                str="　"
                str.decode("utf-8")
                abstract.decode("utf-8") 
                abstract = abstract.replace('/', ';') 

                atlist = abstract.split(str)
                index = 0
                tmpstr = atlist[index].split('<br')[0]
                fieldIndex = 0
                field=nameList[fieldIndex]

                if tmpstr.find('导演') > -1:
                    movie_obj[field] = tmpstr.replace('导演：','')
                    index += 1
                else:
                    movie_obj[field] = ''

                fieldIndex += 1
                field = nameList[fieldIndex]
                if (len(atlist) >= (index + 1)): 
                    tmpstr = atlist[index].split('<br')[0]
                    if tmpstr.find('演员') > -1:
                        actors = tmpstr.split('<br')[0]
                        movie_obj[field] = actors.replace('演员：','')
                        index += 1
                    else:
                        movie_obj[field] = ''

                fieldIndex += 1
                field = nameList[fieldIndex]
                if (len(atlist) >= (index + 1)):
                    tmpstr = atlist[index].split('<br')[0]
                    movie_obj[field] = tmpstr
                    index += 1

                fieldIndex += 1
                field = nameList[fieldIndex]
                if (len(atlist) >= (index + 1)):
                    tmpstr = atlist[index].split('<br')[0]
                    movie_obj[field] = tmpstr
                    index += 1

                for i in range(0,5):
                    field = nameList[i]
                    if not field in  movie_obj.keys():
                        movie_obj[field] = ''

                if (not movie_obj['rater'] is None) and (int(movie_obj['rater']) > 0):
                    movie_obj['class'] = convertRateToClass(movie_obj['rate'])
                    moviefile = temp_dir + "/" + movie_obj['id'] + ".json"
                    mf = open(moviefile, "w")
                    json.dump(movie_obj, mf)
                    #print abstract
                    print "%s,%s,%s,%s,%s,%s" %(movie_obj['director'],movie_obj['actors'],movie_obj['country'],movie_obj['type'],movie_obj['release_year'],movie_obj['class'])
                    output.write( "%s,%s,%s,%s,%s,%s\n" %(movie_obj['director'],movie_obj['actors'],movie_obj['country'],movie_obj['type'],movie_obj['release_year'],movie_obj['class']))

                    for featureName in nameList:
                        feature = movie_obj[featureName]
                        if feature is not None:
                            feature = feature.replace(' ', '')
                            feature = feature.replace('　', '')
                            newFeature = feature.split(';')
                            attr_dict[featureName].extend(newFeature)
        else:
            print "The page %s is empty" % str(pageNum)
    return



def printAttributes(output):
    global nameList
    global attr_dict

    for featureName in nameList:
            attrlist = attr_dict[featureName]
            attrlist = list(set(attrlist))
            print ('@ATTRIBUTE %s {%s}'  % (featureName, ','.join(x for x in attrlist)))
            output.write('@ATTRIBUTE %s {%s}\n'  % (featureName, ','.join(x for x in attrlist)))

def convertRateToClass(ratestr):
    if ratestr is None:
	return 'N'

    rate = float(ratestr)
    if rate >= 0 and rate < 3:
	return 'class_8'
    elif rate >=3 and rate < 5:
	return 'class_7'
    elif rate >= 5 and rate < 6:
	return 'class_6'
    elif rate >=6 and rate < 7:
	return 'class_5'
    elif rate >= 7 and rate < 8:
	return 'class_4'
    elif rate >= 8 and rate < 8.5:
	return 'class_3'
    elif rate >= 8.5 and rate < 9:
	return 'class_2'
    elif rate >= 9 and rate <= 10:
	return 'class_1'


def searchDouban():
    global nameList
    global attr_dict

    url="http://movie.douban.com/category/q"

    config_file, output_file, temp_dir = getConfig()
    
    print output_file
    opener = init(config_file, temp_dir)
    output = open(output_file, "w")
    totalPageNum = 20

    output.write("@RELATION PERSON\n")
    output.write("@DATA\n")

    for fn in nameList:
        attr_dict[fn] = []

    param = {
             #"district":"美国",
             "era":"2013",
             "category":"movie",
             "unwatched":"false",
             "available":"false",
             "sortBy":"date",
             "page": 1,
             "ck":"null",
             #"source":"paginator"
         }

    for num in range(1,totalPageNum):
        param['page'] = num 
        #outputpath = temp_dir + "/" + str(num) + ".json"
        response = sendRequest(opener,url,param,None,None)
        parseResponse(response,output,num,temp_dir)

    printAttributes(output)
    output.close()

    return
if (__name__ == '__main__'):
    searchDouban()
