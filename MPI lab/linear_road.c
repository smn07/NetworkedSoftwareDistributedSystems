#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

/**
 * Group number: 24
 *
 * Group members
 * Marco Barbieri
 * Lucrezia Sorrentino
 * Simone Di Ienno
 *
 **/

// Set DEBUG 1 if you want car movement to be deterministic
#define DEBUG 0

const int num_segments = 256;

const int num_iterations = 1000;
const int count_every = 10;

const double alpha = 0.5;
const int max_in_per_sec = 10;

// Returns the number of car that enter the first segment at a given iteration.
int create_random_input() {
  #if DEBUG
    return 1;
  #else
    return rand() % max_in_per_sec;
  #endif
}

// Returns 1 if a car needs to move to the next segment at a given iteration, 0 otherwise.
int move_next_segment() {
#if DEBUG
  return 1;
#else
  return rand()/RAND_MAX < alpha ? 1 : 0;
#endif
}

int main(int argc, char** argv) { 
  MPI_Init(NULL, NULL);

  int rank;
  int num_procs;
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &num_procs);
  srand(time(NULL) + rank);
  
  // we create the list of segment with an extra segment for the cars that go to another process
  int *segment = (int *) calloc(num_segments/num_procs+1 * sizeof(int), sizeof(int));
  int sum = 0;
  
  // Simulate for num_iterations iterations
  for (int it = 1; it < num_iterations+1; ++it) {
    // Move cars across segments
    for (int i = num_segments/num_procs -1; i >= 0; --i) {
      int moved_cars_num = 0;

      for(int j = 0; j < segment[i]; ++j) {
        moved_cars_num += move_next_segment();
      }

      segment[i] -= moved_cars_num;
      segment[i+1] += moved_cars_num;
    }

    // remove the cars that went to another process
    sum -= segment[num_segments/num_procs];

    if (rank != num_procs-1){
      int rc_send = MPI_Send(&segment[num_segments/num_procs], 1, MPI_INT, rank+1, 0, MPI_COMM_WORLD);

      if (rc_send != MPI_SUCCESS){
        printf("Error sending from %d to %d\n", rank, rank+1);
        fflush(stdout);
        MPI_Finalize();
        return 1;
      }
    }
    
    if (rank != 0){
      int entered_cars;
      int rc_recv = MPI_Recv(&entered_cars, 1, MPI_INT, rank-1, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

      if (rc_recv != MPI_SUCCESS){
        printf("Error receiving from %d to %d\n", rank-1, rank);
        fflush(stdout);
        MPI_Finalize();
        return 1;
      }

      segment[0] += entered_cars;
      sum += entered_cars;
    }

    // New cars may enter in the first segment
    if (rank == 0){
      int entered_cars = create_random_input();
      segment[0] += entered_cars;
      sum += entered_cars;
    }

    // we set to zero the last segment
    segment[num_segments/num_procs] = 0;

    // When needed, compute the overall sum
    if (it%count_every == 0) {
      int global_sum = 0;

      MPI_Reduce(&sum, &global_sum, 1, MPI_INT, MPI_SUM, 0, MPI_COMM_WORLD);
      
      if (rank == 0) {
	      //printf("Iteration: %d, sum: %d\n", it, global_sum);
      }
    }
    
    MPI_Barrier(MPI_COMM_WORLD);
  }

  free(segment);
  segment = NULL;
  
  MPI_Finalize();
}
