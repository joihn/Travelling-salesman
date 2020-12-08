#%%

import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
from os import listdir
from os.path import isfile, join

sns.set()

#%%

def extractData(countries):

    dataFrames=[]
    for name in countries:
        dataFrame = pd.read_csv("./"+name+".csv", sep=";",
                                names=["agent", "num_actions", "reward", "distance_travelled", "reward_per_km"])
        dataFrames.append(dataFrame)
    return dataFrames

countries= ["france"]
dataFrames=extractData(countries)
dataF= dataFrames[0]
agentNames = dataF.agent.unique()

#%%
def get_column(df, agent_name, column, limit_time):
    columna = np.zeros(limit_time)
    columna[:] = df[df.agent==agent_name].head(limit_time)[column]
    return columna

def plot(column, dataF,limit_time):
    f, ax = plt.subplots(1, 1)
    for name in agentNames:
        dataIndividual=get_column(dataF, name, column, limit_time)
        ax.plot(dataIndividual)
    ax.legend(agentNames)
    ax.set_xlabel("num_actions")
    ax.set_ylabel(column)
    plt.show()
#%% // plotting the stuff
plot('reward', dataF, 500)
plot('reward_per_km', dataF, 500)
