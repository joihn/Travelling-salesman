import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author Marcel Dubach and Maxime Gardoni
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	private int x;
	private int y;
	private int direction; // goes from 0 to 3;

	private int energy;

	private static int IDNumber = 0;
	private final int ID;
	private RabbitsGrassSimulationSpace rgSpace;

	private final double randomDirChange = 0.3;

	public RabbitsGrassSimulationAgent(int initEnergy){
		x = -1;
		y = -1;
		energy = initEnergy;
		setDirection();
		IDNumber++;
		ID = IDNumber;
	}

	private void setDirection(){
		direction = (int) (Math.random()*4);
	}

	public void draw(SimGraphics G) {
		if (energy > 5) {
			G.drawFastRoundRect(Color.blue);
		} else {
			G.drawFastRoundRect(Color.red);
		}
	}

	public void setXY(int newX, int newY){
		x = newX;
		y = newY;
	}

	public void setRabbitGrassSpace(RabbitsGrassSimulationSpace rgs){
		rgSpace = rgs;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public String getID(){
		return "Agent-"+ID;
	}

	public int getEnergy(){
		return energy;
	}

	public void setEnergy(int newEnergy){
		energy = newEnergy;
	}

	public void step(){
		if (Math.random()>(1-randomDirChange)){ // change direction in 10% of the cases
			direction = (int) (Math.random()*4);
		}

		Object2DGrid grid = rgSpace.getCurrentAgentSpace();

		int maxiter = 4;

		int newX = 0;
		int newY = 0;

		for(int i=0;i<maxiter;i++){
			if (i>0){
				direction = (direction+1)%4;
			}
			int[] DxDy = getDxDy(direction);
			int dx = DxDy[0];
			int dy = DxDy[1];

			newX = x + dx;
			newY = y + dy;

			newX = (newX + grid.getSizeX())%grid.getSizeX();
			newY = (newY + grid.getSizeY())%grid.getSizeY();

			if(tryMove(newX,newY)){
				System.out.println("Move completed");
				report();
				break;
			}

			if(i==3){ // none of the 4 directions is possible. stay at your place
				newX = x;
				newY = y;
			}
		}

		// store new position in the agent itself
		x = newX;
		y = newY;

		energy -= 1; // decrease energy after step or stay

	}

	public int[] getDxDy(int direction){
		int dx = 0, dy = 0;

		switch (direction){
			case 0: // move right
				dx = 1;
				dy = 0;
				break;
			case 1: // move up
				dx = 0;
				dy = -1;
				break;
			case 2: // move left
				dx = -1;
				dy = 0;
				break;
			case 3: // move down
				dx = 0;
				dy = 1;
				break;
		}
		int[] dxdy = {dx,dy};
		return dxdy;
	}

	public void eatGrass(int grassAmount){
		energy += grassAmount;
	}

	public boolean tryMove(int newX, int newY){
		System.out.println("oldX: "+x+", oldY: "+ y + ", newX: "+ newX + ", newY: "+ newY);
		return rgSpace.moveAgentAt(x,y,newX,newY);
	}

	public void report(){
		System.out.println(	getID() + " at x: " + x + ", y: " + y +
							" has energy level " + getEnergy() + ".");
	}

}
