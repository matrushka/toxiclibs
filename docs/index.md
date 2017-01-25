# Documentation

## `public class VerletPhysics2D`

3D particle physics engine using Verlet integration based on: http://en.wikipedia.org/wiki/Verlet_integration http://www.teknikus.dk/tj/gdc2001.htm

## `public ArrayList<VerletParticle2D> particles`

List of particles

## `public ArrayList<VerletSpring2D> springs`

List of spring/stick connectors

## `protected float timeStep`

Default time step = 1.0

## `protected int numIterations`

Default iterations for verlet solver = 50

## `protected Rect worldBounds`

Optional bounding rect to constrain particles too

## `public VerletPhysics2D()`

Initializes a Verlet engine instance using the default values.

## `public VerletPhysics2D(Vec2D gravity, int numIterations, float drag, float timeStep)`

Initializes an Verlet engine instance with the passed in configuration.

 * **Parameters:**
   * `gravity` — 3D gravity vector
   * `numIterations` — iterations per time step for verlet solver
   * `drag` — drag value 0...1
   * `timeStep` — time step for calculating forces

## `public VerletPhysics2D addParticle(VerletParticle2D p)`

Adds a particle to the list

 * **Parameters:** `p` — 
 * **Returns:** itself

## `public VerletPhysics2D addSpring(VerletSpring2D s)`

Adds a spring connector

 * **Parameters:** `s` — 
 * **Returns:** itself

## `protected void applyConstaints()`

Applies all global constraints and constrains all particle positions to the world bounding rect set.

## `public SpatialIndex<Vec2D> getIndex()`

 * **Returns:** the index

## `public int getNumIterations()`

 * **Returns:** the numIterations

## `public VerletSpring2D getSpring(Vec2D a, Vec2D b)`

Attempts to find the spring element between the 2 particles supplied

 * **Parameters:**
   * `a` — particle 1
   * `b` — particle 2
 * **Returns:** spring instance, or null if not found

## `public float getTimeStep()`

 * **Returns:** the timeStep

## `public Rect getWorldBounds()`

 * **Returns:** the worldBounds

## `public boolean removeParticle(VerletParticle2D p)`

Removes a particle from the simulation.

 * **Parameters:** `p` — particle to remove
 * **Returns:** true, if removed successfully

## `public boolean removeSpring(VerletSpring2D s)`

Removes a spring connector from the simulation instance.

 * **Parameters:** `s` — spring to remove
 * **Returns:** true, if the spring has been removed

## `public boolean removeSpringElements(VerletSpring2D s)`

Removes a spring connector and its both end point particles from the simulation

 * **Parameters:** `s` — spring to remove
 * **Returns:** true, only if spring AND particles have been removed successfully

## `public void setIndex(SpatialIndex<Vec2D> index)`

 * **Parameters:** `index` — the index to set

## `public void setNumIterations(int numIterations)`

 * **Parameters:** `numIterations` — the numIterations to set

## `public void setTimeStep(float timeStep)`

 * **Parameters:** `timeStep` — the timeStep to set

## `public VerletPhysics2D setWorldBounds(Rect world)`

Sets bounding box

 * **Parameters:** `world` — 
 * **Returns:** itself

## `public VerletPhysics2D update()`

Progresses the physics simulation by 1 time step and updates all forces and particle positions accordingly

 * **Returns:** itself

## `protected void updateParticles()`

Updates all particle positions

## `protected void updateSprings()`

Updates all spring connections based on new particle positions