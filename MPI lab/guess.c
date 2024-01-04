#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

int rank;
int num_procs;

const int num_rounds = 1000;

const int min_num = 1;
const int max_num = 1000;

// Array, one element per process
// The leader board, instantiated and used in process 0
int *leaderboard = NULL;

// Array, one element per process
// The array of number selected in the current round
int *selected_numbers = NULL;

// The leader for the current round
int leader = 0;

// Allocate dynamic variables
void allocate_vars() {
  leaderboard = (int *) calloc(num_procs * sizeof(int), sizeof(int));
  selected_numbers = (int *) malloc(num_procs * sizeof(int));
}

// Deallocate dynamic variables
void free_vars() {
  free(leaderboard);
  leaderboard = NULL;
  free(selected_numbers);
  selected_numbers = NULL;
}

// Select a random number between min_num and max_num
int select_number() {
  return min_num + rand() % (max_num - min_num + 1);
}

// Function used to communicate the selected number to the leader
void send_num_to_leader(int num) {
  MPI_Gather(&num, 1, MPI_INT, selected_numbers, 1, MPI_INT, leader, MPI_COMM_WORLD);
}

int min(int *array) {
  int min = array[0];
  for(int i=1; i<num_procs; i++) {
    if(array[i] < min)
      min = array[i];
  }
  return min;
}

int abs(int value){
  if(value > 0)
    return value;
  else
    return -value;
}

// Compute the winner (-1 if there is no winner)
// Function invoked by the leader only
int compute_winner(int number_to_guess) {
  for(int i=0; i<num_procs; i++)
    selected_numbers[i] = abs(selected_numbers[i] - number_to_guess);

  int min_num = min(selected_numbers);

  int count = 0;
  int winner = -1;
  for(int i = 0; i<num_procs; i++) {
    if(selected_numbers[i] == min_num){
      winner = i;
      count++;
    }
  }

  if(count >= 2)
    return -1;
  else
    return winner;
}

// Function used to communicate the winner to everybody
void send_winner(int *winner) {
  MPI_Bcast(winner, 1, MPI_INT, leader, MPI_COMM_WORLD);
}

// Update leader
void update_leader(int winner) {
  if (winner != -1)
    leader = winner;
}

// Update leaderboard (invoked by process 0 only)
void update_leaderboard(int winner) {
  leaderboard[winner]++;
}

// Print the leaderboard
void print_leaderboard(int round, int winner) {
  printf("\n* Round %d *\n", round);
  printf("Winner: %d\n", winner);
  printf("Leaderboard\n");
  for (int i=0; i<num_procs; i++) {
    printf("P%d:\t%d\n", i, leaderboard[i]);
  }
}

int main(int argc, char** argv) { 
  MPI_Init(NULL, NULL);
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &num_procs);
  srand(time(NULL) + rank);

  allocate_vars();

  for (int round=0; round<num_rounds; round++) {
    int selected_number = select_number();
    send_num_to_leader(selected_number);

    int winner;
    if (rank == leader) {
      int num_to_guess = select_number();
      winner = compute_winner(num_to_guess);
    }
    send_winner(&winner);
    update_leader(winner);
    
    if (rank == 0) {
      update_leaderboard(winner);
      print_leaderboard(round, winner);
    }
  }

  MPI_Barrier(MPI_COMM_WORLD);
  free_vars();
  
  MPI_Finalize();
}
