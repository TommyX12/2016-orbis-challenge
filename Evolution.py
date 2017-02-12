import subprocess
import sched, time
import os
import datetime as dt
#from SendKeys import SendKeys
import random

#AUTOMATED = False

s = sched.scheduler(time.time, time.sleep)

#shell = win32com.client.Dispatch("WScript.Shell")
#shell.Run("start Launcher.jar -nogui-l -config Evolved")
#shell.AppActivate("Java")

score = [-1, -1]
chosenMap = ""

total = 0
win = 0

mapList = []
for root, dirs,files in os.walk('./Maps'):
    for fname in files:
        if fname[-4:] == ".png":
            mapList.append(fname[:-4])

def evolve(resultPath):
    global score, chosenMap, total, win

    result = open(resultPath,'r')
    inStr = result.readline();
    left = 17
    right = inStr.find(',', left)
    score1 = int(inStr[left:right])
    left = right+1
    right = inStr.find(']', left)
    score2 = int(inStr[left:right])

    result.close();

    os.remove(resultPath)

    for i in range(2):
        if score[i] == -1:
            score[i] = score2
            break

    print score, score1, score2

    if score2 > score1:
        win += 1
    total += 1

    print str(float(win)*100.0/float(total)) + '%'

    if score[0] != -1 and score[1] != -1:
        if score[0] < score[1]:
            write = open("Bots/Evolved1/PlayerAI.java",'w')
            read = open("Bots/Evolved2/PlayerAI.java",'r')
        else:
            write = open("Bots/Evolved2/PlayerAI.java",'w')
            read = open("Bots/Evolved1/PlayerAI.java",'r')

        outStr = ""
        inStr = "".join(read.readlines())
        left = 0

        while (True):
            right = inStr.find('//#param', left)

            if right == -1: break;

            outStr += inStr[left:right]
            left = right
            right = inStr.find('= ', left) + 2
            outStr += inStr[left:right]
            left = right
            right = inStr.find(';', left)
            paramOld = float(inStr[left:right])

            leftx = inStr.find('p ', left) + 2
            rightx = inStr.find('\n', leftx)
            step = float(inStr[leftx:rightx])
            paramNew = paramOld + (random.random() * 2.0 - 1.0) * step

            outStr += str(paramNew)

            left = right

        outStr += inStr[left:len(inStr)]

        #print outStr

        write.write(outStr)

        read.close();
        write.close();

        score = [-1, -1]
        chosenMap = ""

def setup():
    global score, chosenMap
    matchConfig = open('MatchPresets/Evolution.json','w')

    outStr = "{\"mapName\":\""
    if chosenMap == "":
        chosenMap = mapList[random.randint(0, len(mapList) - 1)]
    outStr += chosenMap
    outStr += "\",\"playerPaths\":["
    if score[0] == -1:
        outStr += "\"Bots/JavaAI2/PlayerAI.java\",\"Bots/Evolved1/PlayerAI.java\""
    else:
        outStr += "\"Bots/JavaAI2/PlayerAI.java\",\"Bots/Evolved2/PlayerAI.java\""
    outStr += "],\"maxResponseTime\":400,\"turnLimit\":100,\"portNumber\":4461,\"serverTimeout\":86400000,\"playerLaunchTypes\":[\"AI\",\"AI\"]}"

    matchConfig.write(outStr)
    matchConfig.close();

running = False

def update(sc):
    global running

    now = dt.datetime.now()
    ago = now-dt.timedelta(minutes=5)

    if running == False:
        setup()
        os.system("start Launcher.jar -nogui-l -config Evolution")
        running = True
    """
    elif AUTOMATED:
        SendKeys('%{TAB}')
        SendKeys('{ESC}')
    """

    for root, dirs,files in os.walk('./Results'):
        for fname in files:
            path = os.path.join(root, fname)
            st = os.stat(path)
            mtime = dt.datetime.fromtimestamp(st.st_mtime)
            if mtime > ago:
                os.system("TASKKILL /F /IM java.exe")
                running = False
                evolve(path)


    s.enter(1, 1, update, (sc, ))


s.enter(1, 1, update, (s, ))

s.run()
