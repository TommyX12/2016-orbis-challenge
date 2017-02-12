import com.orbischallenge.ctz.Constants;
import com.orbischallenge.ctz.objects.ControlPoint;
import com.orbischallenge.ctz.objects.EnemyUnit;
import com.orbischallenge.ctz.objects.FriendlyUnit;
import com.orbischallenge.ctz.objects.Pickup;
import com.orbischallenge.ctz.objects.UnitClient;
import com.orbischallenge.ctz.objects.World;
import com.orbischallenge.ctz.objects.enums.ActivateShieldResult;
import com.orbischallenge.ctz.objects.enums.Direction;
import com.orbischallenge.ctz.objects.enums.MoveResult;
import com.orbischallenge.ctz.objects.enums.PickupResult;
import com.orbischallenge.ctz.objects.enums.PickupType;
import com.orbischallenge.ctz.objects.enums.ShotResult;
import com.orbischallenge.ctz.objects.enums.Team;
import com.orbischallenge.ctz.objects.enums.TileType;
import com.orbischallenge.ctz.objects.enums.UnitAction;
import com.orbischallenge.ctz.objects.enums.UnitCallSign;
import com.orbischallenge.ctz.objects.enums.WeaponType;
import com.orbischallenge.ctz.utils.PointUtils;
import com.orbischallenge.game.engine.Point;
import com.orbischallenge.game.engine.Time;

import java.util.ArrayList;


public class PlayerAI {


    //#param
    public double PARAM_CAPTURE_DEFAULT = 200; //#step 20
    //#param
    public double PARAM_CAPTURE_TARGETED = 500; //#step 50
    //#param
    public double PARAM_CAPTURE_UNCAPTURED = 1000; //#step 100
    //#param
    public double PARAM_CAPTURE_RANGE = 20; //#step 2
    //#param
    public double PARAM_PURSUE_DEFAULT = 200; //#step 20
    //#param
    public double PARAM_PURSUE_TARGETED = 200; //#step 20
    //#param
    public double PARAM_PURSUE_STK = 200; //#step 20
    //#param
    public double PARAM_PURSUE_DISTANCE = 20; //#step 2
    //#param
    public double PARAM_ENGAGE_DEFAULT = 1500; //#step 150
    //#param
    public double PARAM_ENGAGE_STK = 200; //#step 20
    //#param
    public double PARAM_GOPICKUP_DEFAULT = 1500; //#step 150
    //#param
    public double PARAM_GOPICKUP_RANGE = 40; //#step 4
    //#param
    public double PARAM_SHIELD_DEFAULT = 1000; //#step 100
    //#param
    public double PARAM_SHIELD_STK = 500; //#step 50
    //#param
    public double PARAM_CAPTURESPAWN_DEFAULT = 200; //#step 20
    //#param
    public double PARAM_CAPTURESPAWN_TARGETED = 500; //#step 50
    //#param
    public double PARAM_CAPTURESPAWN_UNCAPTURED = 1000; //#step 100
    //#param
    public double PARAM_CAPTURESPAWN_RANGE = 20; //#step 2

    //#end
    
    public int NUM_PLAYERS = 4;
    
    public int TOTAL_STATES = 11;
    public int STATE_IDLE = 0;
    public int STATE_CAPTURE = 1;
    public int STATE_PURSUE = 2;
    public int STATE_ENGAGE = 3;
    public int STATE_PICKUP = 4;
    public int STATE_GOPICKUP = 5;
    public int STATE_RANDMOVE = 6;
    public int STATE_FOLLOW = 7;
    public int STATE_EVADE = 8;
    public int STATE_SHIELD = 9;
    public int STATE_CAPTURESPAWN = 10;
    
    public Direction[] allDirections;
    
    public int[] unitState;

    public double[][] unitStateActivation;
    
    public EnemyUnit[] unitStateEngageTarget;
    public EnemyUnit[] unitStatePursueTarget;
    public EnemyUnit[] unitStateFollowTarget;
    public EnemyUnit[] unitStateEvadeTarget;
    public int[] unitStateRandmoveBuffer;
    public ControlPoint[] unitStateCapturePoint;
    public ControlPoint[] unitStateCapturePointSpawn;
    public Pickup[] unitStatePickup;
    
    public ControlPoint[] controlPoints;
    public Pickup[] pickups;

    public boolean enemyMainframe;
    public boolean friendlyMainframe;

    public PlayerAI() {
        unitState = new int[NUM_PLAYERS];
        for (int i = 0; i < NUM_PLAYERS; ++i){
            unitState[i] = STATE_IDLE;
        }

        unitStateActivation = new double[NUM_PLAYERS][TOTAL_STATES];

        unitStateEngageTarget = new EnemyUnit[NUM_PLAYERS];
        unitStatePursueTarget = new EnemyUnit[NUM_PLAYERS];
        unitStateFollowTarget = new EnemyUnit[NUM_PLAYERS];
        unitStateEvadeTarget = new EnemyUnit[NUM_PLAYERS];
        unitStateRandmoveBuffer = new int[NUM_PLAYERS];
        unitStateCapturePoint = new ControlPoint[NUM_PLAYERS];
        unitStateCapturePointSpawn = new ControlPoint[NUM_PLAYERS];
        unitStatePickup = new Pickup[NUM_PLAYERS];
        
        //initialize directions.
        allDirections = new Direction[8];
        allDirections[0] = Direction.EAST;
        allDirections[1] = Direction.WEST;
        allDirections[2] = Direction.NORTH;
        allDirections[3] = Direction.SOUTH;
        allDirections[4] = Direction.NORTH_WEST;
        allDirections[5] = Direction.NORTH_EAST;
        allDirections[6] = Direction.SOUTH_WEST;
        allDirections[7] = Direction.SOUTH_EAST;
    }
    
    public boolean unitAlive(UnitClient unit){
        if (unit == null) return false;
        return unit.getHealth() > 0;
    }
    
    public boolean unitShouldPickup(FriendlyUnit unit, Pickup pickup){
        if (pickup == null) return false;
        
        PickupType type = pickup.getPickupType();
        if (type == PickupType.WEAPON_MINI_BLASTER) return false;
        
        if (unit.getCurrentWeapon() != WeaponType.MINI_BLASTER){
            if (type == PickupType.WEAPON_LASER_RIFLE || type == PickupType.WEAPON_RAIL_GUN || type == PickupType.WEAPON_SCATTER_GUN){
                return false;
            }
        }
        
        return true;
    }
    
    public int min(int a, int b){
        return a < b ? a : b;
    }

    public int randInt(int a, int b){
        return (int)Math.floor(Math.random() * (b - a) + a);
    }

    public void obtainInformation(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits){

        controlPoints = world.getControlPoints();
        pickups = world.getPickups();

        friendlyMainframe = enemyMainframe = false;

        for (ControlPoint point: controlPoints){
            if (point.getControllingTeam().equals(friendlyUnits[0].getTeam()) && point.isMainframe()){
                friendlyMainframe = true;
            }
            else if (point.getControllingTeam().equals(enemyUnits[0].getTeam()) && point.isMainframe()){
                enemyMainframe = true;
            }
        }

    }

    public int shotsToKill(UnitClient shooter, UnitClient target){
        return (int)Math.ceil((double)target.getHealth() / (double)shooter.getCurrentWeapon().getDamage());
    }

    public void evaluateStates(int unitIndex, World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits){
            
        FriendlyUnit unit = friendlyUnits[unitIndex];

        for (int i = 0; i < TOTAL_STATES; ++i){
            unitStateActivation[unitIndex][i] = 0.0;
        }

        if (unitAlive(unit)){
            WeaponType weapon = unit.getCurrentWeapon();

            //engage
            for (Direction direction : allDirections){
                EnemyUnit enemy = world.getClosestShootableEnemyInDirection(unit, direction);
                
                if (!unitAlive(enemy) || !world.canShooterShootTarget(unit.getPosition(), enemy.getPosition(), weapon.getRange())) continue;
                
                double activation = PARAM_ENGAGE_DEFAULT;
            
                ///*
                boolean targeted = false;
                
                for (int j = 0; j < NUM_PLAYERS; ++j){
                    if (j != unitIndex && unitState[j] == STATE_ENGAGE && unitStateEngageTarget[j] == enemy) {
                        targeted = true;
                        break;
                    }
                }
                
                if (targeted) activation += 1200;

                if (unit.getLastMoveResult() == MoveResult.BLOCKED_BY_ENEMY || unit.getLastMoveResult() == MoveResult.BLOCKED_BY_FRIENDLY){
                    activation += 99999999;
                }

                activation -= Math.min((shotsToKill(unit, enemy) - shotsToKill(enemy, unit)) * PARAM_ENGAGE_STK, 500);

                //*/

                if (unit.getShieldedTurnsRemaining() > 0) activation = 0;
                if (enemy.getShieldedTurnsRemaining() > 0) activation = 0;
                if (!friendlyMainframe && enemyMainframe) activation -= 400;

                if (activation > unitStateActivation[unitIndex][STATE_ENGAGE]){
                    unitStateActivation[unitIndex][STATE_ENGAGE] = activation;
                    unitStateEngageTarget[unitIndex] = enemy;
                }
            }


            //pickup
            if (unitShouldPickup(unit, world.getPickupAtPosition(unit.getPosition()))){
                unitStateActivation[unitIndex][STATE_PICKUP] = 99999999;
            }

            //randmove
            if (unit.getLastMoveResult() == MoveResult.BLOCKED_BY_ENEMY || unit.getLastMoveResult() == MoveResult.BLOCKED_BY_FRIENDLY){
                if (unitStateRandmoveBuffer[unitIndex] >= 2 || unit.getShieldedTurnsRemaining() > 0){
                    unitStateActivation[unitIndex][STATE_RANDMOVE] = 999999;
                }
                unitStateRandmoveBuffer[unitIndex]++;
            }
            else {
                unitStateRandmoveBuffer[unitIndex] = 0;
            }

            //shield
            for (EnemyUnit enemy: enemyUnits){
                if (!unitAlive(enemy) || !world.canShooterShootTarget(enemy.getPosition(), unit.getPosition(), enemy.getCurrentWeapon().getRange())) continue;
                
                double activation = PARAM_SHIELD_DEFAULT;

                activation += (shotsToKill(unit, enemy) - shotsToKill(enemy, unit)) * PARAM_SHIELD_STK;
                if (!friendlyMainframe && enemyMainframe) activation += 1000;
                if (unit.getShieldedTurnsRemaining() > 0) activation = 0;
                if (enemy.getShieldedTurnsRemaining() > 0) activation = 0;
                if (unit.getNumShields() == 0) activation = 0;

                if (activation > unitStateActivation[unitIndex][STATE_SHIELD]){
                    unitStateActivation[unitIndex][STATE_SHIELD] = activation;
                }
            }
            
            //gopickup
            for (Pickup pickup : pickups){
                
                if (!unitShouldPickup(unit, pickup)) continue;

                double activation = PARAM_GOPICKUP_DEFAULT;
                
                boolean targeted = false;
                
                for (int j = 0; j < NUM_PLAYERS; ++j){
                    if (j != unitIndex && unitState[j] == STATE_GOPICKUP && unitStatePickup[j] == pickup) {
                        targeted = true;
                        break;
                    }
                }
                
                if (targeted) activation = 0;
                
                int distance = world.getPathLength(unit.getPosition(), pickup.getPosition());
                activation -= Math.min(distance * PARAM_GOPICKUP_RANGE, 800);

                if (activation > unitStateActivation[unitIndex][STATE_GOPICKUP]){
                    unitStateActivation[unitIndex][STATE_GOPICKUP] = activation;
                    unitStatePickup[unitIndex] = pickup;
                }
            }

            //capture
            for (ControlPoint point: controlPoints){
                double activation = PARAM_CAPTURE_DEFAULT;

                boolean targeted = false;
                
                for (int j = 0; j < NUM_PLAYERS; ++j){
                    if (j != unitIndex && unitState[j] == STATE_CAPTURE && unitStateCapturePoint[j] == point) {
                        targeted = true;
                        break;
                    }
                }

                if (targeted) activation -= PARAM_CAPTURE_TARGETED;

                if (!point.getControllingTeam().equals(unit.getTeam())) activation += PARAM_CAPTURE_UNCAPTURED;
                
                int distance = world.getPathLength(unit.getPosition(), point.getPosition());
                activation -= Math.min(distance * PARAM_CAPTURE_RANGE, 500);

                if (activation > unitStateActivation[unitIndex][STATE_CAPTURE]){
                    unitStateActivation[unitIndex][STATE_CAPTURE] = activation;
                    unitStateCapturePoint[unitIndex] = point;
                }
            }

            //capture spawn
            for (ControlPoint point: controlPoints){

                if (!point.isMainframe()) continue;

                double activation = PARAM_CAPTURESPAWN_DEFAULT;

                boolean targeted = false;
                
                for (int j = 0; j < NUM_PLAYERS; ++j){
                    if (j != unitIndex && unitState[j] == STATE_CAPTURESPAWN && unitStateCapturePointSpawn[j] == point) {
                        targeted = true;
                        break;
                    }
                }

                if (!point.getControllingTeam().equals(unit.getTeam())) {
                    activation += PARAM_CAPTURESPAWN_UNCAPTURED;
                    if (targeted) activation += PARAM_CAPTURESPAWN_TARGETED;
                    if (!friendlyMainframe) activation += 1000;
                }
                
                int distance = world.getPathLength(unit.getPosition(), point.getPosition());
                activation -= Math.min(distance * PARAM_CAPTURESPAWN_RANGE, 500);

                if (activation > unitStateActivation[unitIndex][STATE_CAPTURESPAWN]){
                    unitStateActivation[unitIndex][STATE_CAPTURESPAWN] = activation;
                    unitStateCapturePointSpawn[unitIndex] = point;
                }
            }
            
            //pursue
            for (EnemyUnit enemy: enemyUnits){
                
                if (!unitAlive(enemy)) continue;

                double activation = PARAM_PURSUE_DEFAULT;
                
                ///*
                boolean targeted = false;
                
                for (int j = 0; j < NUM_PLAYERS; ++j){
                    if (j != unitIndex && unitState[j] == STATE_ENGAGE && unitStateEngageTarget[j] == enemy) {
                        targeted = true;
                        break;
                    }
                }
                
                if (targeted) activation += PARAM_PURSUE_TARGETED;

                activation -= Math.min((shotsToKill(unit, enemy) - shotsToKill(enemy, unit)) * PARAM_PURSUE_STK, 500);

                //*/
                
                int distance = world.getPathLength(unit.getPosition(), enemy.getPosition());
                activation -= Math.min(distance * PARAM_PURSUE_DISTANCE, 400);

                if (unit.getShieldedTurnsRemaining() > 0) activation = 0;
                if (enemy.getShieldedTurnsRemaining() > 0) activation = 0;
                if (!friendlyMainframe && enemyMainframe) activation = 0;

                if (activation > unitStateActivation[unitIndex][STATE_PURSUE]){
                    unitStateActivation[unitIndex][STATE_PURSUE] = activation;
                    unitStatePursueTarget[unitIndex] = enemy;
                }
            }


        }

    }

    public void executeState(int unitIndex, World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits){
            
        FriendlyUnit unit = friendlyUnits[unitIndex];

        if (unitAlive(unit)){

            ArrayList<Integer> maxStates = new ArrayList<Integer>();
            maxStates.add(STATE_IDLE);

            double maxStateActivation = 0.0;

            System.out.println(unitIndex);

            for (int i = 0; i < TOTAL_STATES; ++i){
                double stateActivation = unitStateActivation[unitIndex][i];
                System.out.println(stateActivation);
                if (stateActivation > maxStateActivation){
                    maxStateActivation = stateActivation;
                    maxStates.clear();
                    maxStates.add(i);
                }
                else if (stateActivation == maxStateActivation){
                    maxStates.add(i);
                }
            }

            int maxState = maxStates.get(randInt(0, maxStates.size()));

            //do stuff with maxState

            if (maxState == STATE_CAPTURE){
                unit.move(unitStateCapturePoint[unitIndex].getPosition());
            }
            else if (maxState == STATE_PURSUE){
                unit.move(unitStatePursueTarget[unitIndex].getPosition());
            }
            else if (maxState == STATE_ENGAGE){
                unit.shootAt(unitStateEngageTarget[unitIndex]);
            }
            else if (maxState == STATE_PICKUP){
                unit.pickupItemAtPosition();
            }
            else if (maxState == STATE_GOPICKUP){
                unit.move(unitStatePickup[unitIndex].getPosition());
            }
            else if (maxState == STATE_RANDMOVE){
                unit.move(allDirections[randInt(0, 8)]);
            }
            else if (maxState == STATE_FOLLOW){
                unit.move(unitStateFollowTarget[unitIndex].getPosition());
            }
            else if (maxState == STATE_SHIELD){
                unit.activateShield();
            }
            else if (maxState == STATE_CAPTURESPAWN){
                unit.move(unitStateCapturePointSpawn[unitIndex].getPosition());
            }

            unitState[unitIndex] = maxState;
        }
        else {
            unitState[unitIndex] = STATE_IDLE;
        }
    }

    /**
     * This method will get called every turn.
     *
     * @param world The latest state of the world.
     * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
     * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
     */
    public void doMove(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits) {
        
        obtainInformation(world, enemyUnits, friendlyUnits);
        for (int i = 0; i < NUM_PLAYERS; ++i){
            evaluateStates(i, world, enemyUnits, friendlyUnits);
            executeState(i, world, enemyUnits, friendlyUnits);
        }

    }
}
