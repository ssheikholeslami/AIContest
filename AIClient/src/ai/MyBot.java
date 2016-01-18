package ai;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

import config.Config;
import game.*;
import game.Unit.*;
import io.Command.Direction;

public class MyBot extends Bot
{



	public class SuperUnit{

		private Unit soul;
		private int agentID;
		private int goalDirection;
		private boolean isMissionDone = false;

		public SuperUnit(Unit unit){

			this.soul = unit;
			this.agentID = maxAgentID;
			maxAgentID++;
		}

		public void setGoalDirection(){

			int result = getAgentID() % 4;
			setGoalDirection(result);
		}

		public int getGoalDirection() {
			return goalDirection;
		}

		public void setGoalDirection(int goalDirection) {
			this.goalDirection = goalDirection;
		}

		public int getAgentID() {
			return agentID;
		}

		public void setAgentID(int agentID) {
			this.agentID = agentID;
		}

		public Unit getSoul() {
			return soul;
		}

		public boolean isMissionDone() {
			return isMissionDone;
		}

		public void setIsMissionDone(boolean isMissionDone) {
			this.isMissionDone = isMissionDone;
		}
	}

	private Random rand;
	public boolean isFirstUnitKilled = false;
	public final short DIRECTION_NORTH = 0, DIRECTION_EAST = 1, DIRECTION_SOUTH = 2, DIRECTION_WEST = 3;
	public short GUESSED_ENEMY_BASE_DIR;
	public int maxAgentID = 0;

	public ArrayList<SuperUnit> listOfSuperUnits = new ArrayList<SuperUnit>();
	public boolean FLAG_SUPERUNITASSIGNMENT = false;

	@Override
	public void think() {


		rand = new Random();
		//TODO handle if can't continue its goalDirection (e.g. reaches a wall or end of map)

		// exploration phase

		if(!isFirstUnitKilled || getEnemyAgents().getHQsList().size()==0) {
			if (getSelfAgents().getAllUnits().size() < 5) {

				for (Building.HeadQuarters hq : getSelfAgents().getHQsList()) {
					if(getSuppliesAmount() >= Config.Unit.Melee.creationCost)
							hq.spawnUnit(UnitType.MELEE);
				}

				if (listOfSuperUnits.size()<4){

					for(Unit unit: getSelfAgents().getMeleeList()){
						boolean assigned  = false;
						for(SuperUnit superUnit: listOfSuperUnits){
							if(superUnit.getSoul().getID() == unit.getID()){
								assigned = true;
								break;
							}
						}
						if(!assigned){
							SuperUnit superUnit = new SuperUnit(unit);
							superUnit.setAgentID(listOfSuperUnits.size());
							superUnit.setGoalDirection();

//							System.out.println("Direction SET: " + superUnit.getGoalDirection());

							listOfSuperUnits.add(superUnit);
//							System.out.println("SuperUnit Added!");
						}

					}

				}
//				randomWalk();
				pioneerWalk();
			}

			else{




			}




//			dummyThink();



		}
		else{

		}

	}

	private void pioneerWalk() {


		for(SuperUnit superUnit: listOfSuperUnits) {

			if (superUnit.isMissionDone()) {
				randomWalkUnit(superUnit.getSoul());
			} else {
				Direction dir;

				switch (superUnit.getGoalDirection()) {

					case DIRECTION_NORTH:
						dir = Direction.NORTH;
						break;
					case DIRECTION_EAST:
						dir = Direction.EAST;
						break;
					case DIRECTION_SOUTH:
						dir = Direction.SOUTH;
						break;
					case DIRECTION_WEST:
						dir = Direction.WEST;
						break;
					default:
						dir = Direction.getDirByNum(rand.nextInt());
						break;
				}

				// check walkable
				if (getGameField().isWalkable(superUnit.getSoul().getX() + dir.deltaX, superUnit.getSoul().getY() + dir.deltaY)) {
					// check if there is no unit
					if (getSelfAgents().getUnitAt(superUnit.getSoul().getX() + dir.deltaX, superUnit.getSoul().getY() + dir.deltaY) == null) {
						superUnit.getSoul().move(dir);
					}

				} else {
					if (getGameField().isOutOfField(superUnit.getSoul().getX() + dir.deltaX, superUnit.getSoul().getY() + dir.deltaY)) {

						// mission done, random exploration
						superUnit.setIsMissionDone(true);
						randomWalkUnit(superUnit.getSoul());

					} else {
						while (!getGameField().isWalkable(superUnit.getSoul().getX() + dir.deltaX, superUnit.getSoul().getY() + dir.deltaY)) {
							dir = Direction.getDirByNum(rand.nextInt());
						}

						superUnit.getSoul().move(dir);

					}
				}

			}

		}
	}

	private void randomWalk() {



		//list all supplies' positions
		ArrayList<Point> supplies = new ArrayList<Point>();
		for (int i = 0 ; i < getGameField().getWidth() ; i++) {
			for (int j = 0 ; j < getGameField().getHeight() ; j++) {
				if (getGameField().isSuppliesAt(i, j)) {
					supplies.add((new Point(i, j)));
				}
			}
		}

		// gather nearby supplies, or move randomly if there is none
		for (Unit unit : getSelfAgents().getAllUnits()) {
			if(getGameField().isSuppliesAt(unit.getX() + 1, unit.getY())) {
				unit.move(Direction.EAST);
			} else if(getGameField().isSuppliesAt(unit.getX() - 1, unit.getY())) {
				unit.move(Direction.WEST);
			} else if(getGameField().isSuppliesAt(unit.getX(), unit.getY() + 1)) {
				unit.move(Direction.SOUTH);
			} else if(getGameField().isSuppliesAt(unit.getX(), unit.getY() - 1)) {
				unit.move(Direction.NORTH);
			} else {
				// pick a random goalDirection and make sure it is walkable(there is no
				// wall there) and no friendly unit is already there

				Direction dir;
				dir = Direction.getDirByNum(rand.nextInt());

				// check walkable
				if(getGameField().isWalkable(unit.getX() + dir.deltaX, unit.getY() + dir.deltaY)) {
					// check if there is no unit
					if(getSelfAgents().getUnitAt(unit.getX() + dir.deltaX, unit.getY() + dir.deltaY) == null) {
						unit.move(dir);
					}
				}
			}
		}

		// attack everything you can see!!
		// (note that each agent will only run the LAST command given to it,
		// meaning they will ignore the move command and run the attack command
		// if possible)
		ArrayList<Unit> enemyUnits = getEnemyAgents().getAllUnits();
		ArrayList<Building.HeadQuarters> enemyBuildings = getEnemyAgents().getHQsList();

		for(Unit unit : getSelfAgents().getAllUnits()) {
			for(Unit enemyUnit : enemyUnits) {
				if(unit.isInAttackRange(enemyUnit)) {
					unit.attack(enemyUnit);
				}
			}

			for(Building enemyBuilding : enemyBuildings) {
				if(unit.isInAttackRange(enemyBuilding)) {
					unit.attack(enemyBuilding);
				}
			}
		}



	}


	private void randomWalkUnit(Unit unit) {



		//list all supplies' positions
		ArrayList<Point> supplies = new ArrayList<Point>();
		for (int i = 0 ; i < getGameField().getWidth() ; i++) {
			for (int j = 0 ; j < getGameField().getHeight() ; j++) {
				if (getGameField().isSuppliesAt(i, j)) {
					supplies.add((new Point(i, j)));
				}
			}
		}

		// gather nearby supplies, or move randomly if there is none
			if(getGameField().isSuppliesAt(unit.getX() + 1, unit.getY())) {
				unit.move(Direction.EAST);
			} else if(getGameField().isSuppliesAt(unit.getX() - 1, unit.getY())) {
				unit.move(Direction.WEST);
			} else if(getGameField().isSuppliesAt(unit.getX(), unit.getY() + 1)) {
				unit.move(Direction.SOUTH);
			} else if(getGameField().isSuppliesAt(unit.getX(), unit.getY() - 1)) {
				unit.move(Direction.NORTH);
			} else {
				// pick a random goalDirection and make sure it is walkable(there is no
				// wall there) and no friendly unit is already there

				Direction dir;
				dir = Direction.getDirByNum(rand.nextInt());

				// check walkable
				if(getGameField().isWalkable(unit.getX() + dir.deltaX, unit.getY() + dir.deltaY)) {
					// check if there is no unit
					if(getSelfAgents().getUnitAt(unit.getX() + dir.deltaX, unit.getY() + dir.deltaY) == null) {
						unit.move(dir);
					}
				}
			}


		// attack everything you can see!!
		// (note that each agent will only run the LAST command given to it,
		// meaning they will ignore the move command and run the attack command
		// if possible)
		ArrayList<Unit> enemyUnits = getEnemyAgents().getAllUnits();
		ArrayList<Building.HeadQuarters> enemyBuildings = getEnemyAgents().getHQsList();


			for(Unit enemyUnit : enemyUnits) {
				if(unit.isInAttackRange(enemyUnit)) {
					unit.attack(enemyUnit);
				}
			}

			for(Building enemyBuilding : enemyBuildings) {
				if(unit.isInAttackRange(enemyBuilding)) {
					unit.attack(enemyBuilding);
				}
			}




	}


	public void dummyThink()
	{
		// place your code here.
		// here is a very basic sample of what you can do:
		Random rand = new Random();


		// spawn some random units, if you have enough supplies
		for(Building.HeadQuarters hq : getSelfAgents().getHQsList()) {
			if (new Random().nextBoolean()) {
				if(getSuppliesAmount() >= Config.Unit.Ranged.creationCost) {
					hq.spawnUnit(UnitType.RANGED);
				}
			} else {
				if(getSuppliesAmount() >= Config.Unit.Melee.creationCost) {
					hq.spawnUnit(UnitType.MELEE);
				}
			}
		}


		// list all friendly units
		ArrayList<Unit> units = getSelfAgents().getAllUnits();

		// list friendly ranged units:
		ArrayList<Unit> rangedUnits = getSelfAgents().getRangedList();
		
		// list friendly melee units:
		ArrayList<Unit> meleeUnits = getSelfAgents().getMeleeList();
		
		//list all supplies' positions
		ArrayList<Point> supplies = new ArrayList<Point>();
		for (int i = 0 ; i < getGameField().getWidth() ; i++) {
			for (int j = 0 ; j < getGameField().getHeight() ; j++) {
				if (getGameField().isSuppliesAt(i, j)) {
					supplies.add((new Point(i, j)));
				}
			}
		}
		
		// gather nearby supplies, or move randomly if there is none
		for (Unit unit : units) {
			if(getGameField().isSuppliesAt(unit.getX() + 1, unit.getY())) {
				unit.move(Direction.EAST);
			} else if(getGameField().isSuppliesAt(unit.getX() - 1, unit.getY())) {
				unit.move(Direction.WEST);
			} else if(getGameField().isSuppliesAt(unit.getX(), unit.getY() + 1)) {
				unit.move(Direction.SOUTH);
			} else if(getGameField().isSuppliesAt(unit.getX(), unit.getY() - 1)) {
				unit.move(Direction.NORTH);
			} else {
				// pick a random goalDirection and make sure it is walkable(there is no
				// wall there) and no friendly unit is already there

				Direction dir;
				dir = Direction.getDirByNum(rand.nextInt());

				// check walkable
				if(getGameField().isWalkable(unit.getX() + dir.deltaX, unit.getY() + dir.deltaY)) {
					// check if there is no unit
					if(getSelfAgents().getUnitAt(unit.getX() + dir.deltaX, unit.getY() + dir.deltaY) == null) {
						unit.move(dir);
					}
				}
			}
		}

		// attack everything you can see!!
		// (note that each agent will only run the LAST command given to it,
		// meaning they will ignore the move command and run the attack command
		// if possible)
		ArrayList<Unit> enemyUnits = getEnemyAgents().getAllUnits();
		ArrayList<Building.HeadQuarters> enemyBuildings = getEnemyAgents().getHQsList();

		for(Unit unit : units) {
			for(Unit enemyUnit : enemyUnits) {
				if(unit.isInAttackRange(enemyUnit)) {
					unit.attack(enemyUnit);
				}
			}

			for(Building enemyBuilding : enemyBuildings) {
				if(unit.isInAttackRange(enemyBuilding)) {
					unit.attack(enemyBuilding);
				}
			}
		}
	}
}
