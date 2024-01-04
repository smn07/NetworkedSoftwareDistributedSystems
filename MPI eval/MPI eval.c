#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <mpi.h>
#include <math.h>

/*
 * Group number: 24
 *
 * Group members
 *  - Marco Barbieri
 *  - Lucrezia Sorrentino
 *  - Simone Di Ienno
 */

#define DEBUG 0

const float min = 0;
const float max = 1000;
const float len = max - min;
#if DEBUG == 1
const int num_ants = 40;
#else
const int num_ants = 8 * 1000 * 1000;
#endif
const int num_food_sources = 10;
const int num_iterations = 500;

float random_position() {
  return (float) rand() / (float)(RAND_MAX/(max-min)) + min;
}

/*
 * Process 0 invokes this function to initialize food sources.
 */
void init_food_sources(float* food_sources) {
  for (int i=0; i<num_food_sources; i++) {
    #if DEBUG == 1
      food_sources[i] = i*(max-min)/num_food_sources;
    #else
      food_sources[i] = random_position();
    #endif
  }
}

/*
 * Process 0 invokes this function to initialize the position of ants.
 */
void init_ants(float* ants) {
  for (int i=0; i<num_ants; i++) {
    #if DEBUG == 1
      ants[i] = i*(max-min)/num_ants;
    #else
      ants[i] = random_position();
    #endif
  }
}

float compute_center(float* ants, int ants_per_proc){
    float local_sum = 0;
    float global_sum;

    for(int i=0; i<ants_per_proc; i++)
      local_sum += ants[i];

    // we call a reduce to sum all the local sums and store the result in global_sum
    // we use MPI_Allreduce because we want to have the result in all the processes
    int rc_allreduce = MPI_Allreduce(&local_sum, &global_sum, 1, MPI_FLOAT, MPI_SUM, MPI_COMM_WORLD);
    if (rc_allreduce != MPI_SUCCESS) {
      printf("Error while reducing the local sums\n");
      fflush(stdout);
      MPI_Abort(MPI_COMM_WORLD, rc_allreduce);
    }

    return global_sum / num_ants;
}

void compute_first_force(float* ants, float* food_sources, int ants_per_proc, float* forces){
  for(int i=0; i<ants_per_proc; i++) {
    // find the closest food source
    float min_distance = fabs(ants[i] - food_sources[0]);
    int min_distance_index = 0;

    for(int j=1; j<num_food_sources; j++) {
      float distance = fabs(ants[i] - food_sources[j]);

      if (distance < min_distance) {
        min_distance = distance;
        min_distance_index = j;
      }
    }

    // we take the signed distance to calculate the force
    forces[i] = (food_sources[min_distance_index] - ants[i])*0.01;
  }
}

void compute_second_force(float* ants, float center, int ants_per_proc, float* forces){
  for(int i=0; i<ants_per_proc; i++) {
    forces[i] += (center - ants[i])*0.012;
  }
}

int main() {
  MPI_Init(NULL, NULL);
    
  int rank;
  int num_procs;
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &num_procs);

  srand(rank);

  // Allocate space in each process for food sources and ants
  int ants_per_proc = num_ants/num_procs;

  float* ants = (float*) malloc(ants_per_proc * sizeof(float));
  float* food_sources = (float*) malloc(num_food_sources * sizeof(float));
  
  // Process 0 initializes food sources and ants
  float* initial_ants;
  if (rank == 0) {
    initial_ants = (float*) malloc(num_ants * sizeof(float));
    init_ants(initial_ants);
    init_food_sources(food_sources);
  }

  // Process 0 distributes food sources and ants

  // we broadcast the food sources to all the processes so each process has all the food sources
  // because each process needs to calculate the first force for each ant and this force depends on the position
  // of the food sources
  int rc_broadcast = MPI_Bcast(food_sources, num_food_sources, MPI_FLOAT, 0, MPI_COMM_WORLD);
  if (rc_broadcast != MPI_SUCCESS) {
    printf("Error while broadcasting the food sources\n");
    fflush(stdout);
    MPI_Abort(MPI_COMM_WORLD, rc_broadcast);
  }

  // we scatter the ants to all the processes so each process has a part of the ants
  int rc_scatter = MPI_Scatter(initial_ants, ants_per_proc, MPI_FLOAT, ants, ants_per_proc, MPI_FLOAT, 0, MPI_COMM_WORLD);
  if (rc_scatter != MPI_SUCCESS) {
    printf("Error while scattering the ants to the processes\n");
    fflush(stdout);
    MPI_Abort(MPI_COMM_WORLD, rc_scatter);
  }

  #if DEBUG == 1
    printf("Process %d - Food sources: ", rank);
    for(int i=0; i<num_food_sources; i++) {
      printf("%f ", food_sources[i]);
    }
    printf("\n");

    printf("Process %d - Ants: ", rank);
    for(int i=0; i<ants_per_proc; i++) {
      printf("%f ", ants[i]);
    }
    printf("\n");
    fflush(stdout);
  #endif
  
  // Iterative simulation
  float center = 0;
  // calculate the initial center of the colony
  center = compute_center(ants, ants_per_proc);

  float* forces = (float*) calloc(ants_per_proc, sizeof(float));

  for (int iter=0; iter<num_iterations; iter++) {
    #if DEBUG == 1
      printf("Process %d - Center: %f\n", rank, center);
      fflush(stdout);
    #endif

    // calcualte first force
    compute_first_force(ants, food_sources, ants_per_proc, forces);

    // calculate the second force
    compute_second_force(ants, center, ants_per_proc, forces);

    // update ants position
    for(int i=0; i<ants_per_proc; i++) {
      ants[i] += forces[i];
    }

    // calculate the center of the colony
    center = compute_center(ants, ants_per_proc);
    
    if (rank == 0) {
      printf("Iteration: %d - Average position: %f\n", iter, center);
      fflush(stdout);
    }

    // we don't need a barrier because in the compute center function there is an allreduce
    // and it is an implicit barrier, so the positions of the ants are already updated for the next iteration
  }

  // Free memory
  if(rank == 0)
    free(initial_ants);

  free(ants);
  free(food_sources);
  free(forces);

  MPI_Barrier(MPI_COMM_WORLD);
  
  MPI_Finalize();
  return 0;
}
