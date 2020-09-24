/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author Marcel Dubach, Maxime Gardoni
 * The object contains 2 fields for 2D grid objects for the rabbit space and the grass space
 */
import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
    private Object2DGrid grassSpace;
    private Object2DGrid rabbitSpace;

    public RabbitsGrassSimulationSpace(int xSize, int ySize){
        grassSpace = new Object2DGrid(xSize, ySize);
        rabbitSpace = new Object2DGrid(xSize,ySize);
        for(int i = 0; i < xSize; i++){
            for(int j = 0 ; j< ySize; j++){
                grassSpace.putObjectAt(i,j,new Integer(0)); // initialize grass space to 0
                rabbitSpace.putObjectAt(i,j,null); // initialize rabbit space to null
            }
        }
    }

    // spreadGrass is perfomed once to seed some initial grass
    // adds numGrass units to the grassSpace (at random places)
    public void spreadGrass(int numGrass){
        int grass = 0;
        for (int ngrass = 0; ngrass < numGrass; ngrass++){
            int x = (int) (Math.random()*(grassSpace.getSizeX()));
            int y = (int) (Math.random()*(grassSpace.getSizeY()));
            grass =  getGrassAt(x,y);
            grassSpace.putObjectAt(x,y,new Integer(grass+1));
        }
    }

    // growGrass grows grass continuously during simulation
    // loops over the entire field and addy one unit at the given percentage
    public void growGrass(double growthRate){
        int grass = 0;
        for(int i=0;i<grassSpace.getSizeX();i++){
            for(int j=0;j<grassSpace.getSizeY();j++){
                if(Math.random()<growthRate){
                    grass = getGrassAt(i,j);
                    if ((grass+1)<=255){
                        grassSpace.putObjectAt(i,j,new Integer(grass + 1));
                    }
                }
            }
        }
    }

    // get grass value at specific place
    public int getGrassAt(int x, int y){
        int grass;
        if (grassSpace.getObjectAt(x,y) != null) {
            grass = ((Integer) grassSpace.getObjectAt(x, y)).intValue();
        } else {
            grass = 0;
        }
        return grass;
    }

    // count grass over the entire field
    public int countGrass(){
        int nGrasses = 0;
        for(int i=0;i<grassSpace.getSizeX();i++){
            for(int j=0;j<grassSpace.getSizeY();j++){
                nGrasses += getGrassAt(i,j);
            }
        }
        return nGrasses;
    }

    // set grass value to zero at specific place
    public void removeGrassAt(int x, int y){
        grassSpace.putObjectAt(x,y,new Integer(0));
    }

    // remove agent at specific place
    public void removeAgentAt(int x, int y){
        rabbitSpace.putObjectAt(x,y,null); // set null
    }

    //
    public boolean moveAgentAt(int x, int y, int newX, int newY){
        boolean moveSuccessful = false;
        if (!isOccupiedByRabbit(newX,newY)){
            RabbitsGrassSimulationAgent rgAgent = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(x,y);
            removeAgentAt(x,y);
            rgAgent.setXY(newX,newY);
            rabbitSpace.putObjectAt(newX,newY,rgAgent);
            moveSuccessful = true;
        }
        return moveSuccessful;
    }

    public Object2DGrid getCurrentAgentSpace(){
        return rabbitSpace;
    }


    public Object2DGrid getCurrentGrassSpace(){
        return grassSpace;
    }

    public boolean isOccupiedByRabbit(int x, int y){
        boolean isOccupied = false;
        if(rabbitSpace.getObjectAt(x,y) != null){
            isOccupied = true;
        }
        return isOccupied;
    }


    public boolean addRabbit(RabbitsGrassSimulationAgent rabbit){
        boolean addSuccess = false;
        int count = 0;
        int countLimit = 10;
        // random initialisation of rabbit position (int rabbitSpace and each agent itself)
        while((addSuccess==false)&&(count<countLimit)){
            int x = (int) (Math.random()*(rabbitSpace.getSizeX()));
            int y = (int) (Math.random()*(rabbitSpace.getSizeY()));
            if(isOccupiedByRabbit(x,y) == false){
                rabbitSpace.putObjectAt(x,y,rabbit);
                rabbit.setXY(x,y);
                rabbit.setRabbitGrassSpace(this);
                addSuccess = true;
            }
            count++;
        }
        return addSuccess;
    }


}
