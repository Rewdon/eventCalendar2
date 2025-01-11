package com.example.eventcalendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> taskList; // Lista zadań
    private ArrayAdapter<String> adapter; // Adapter do ListView
    private LinearLayout addTaskLayout; // Układ dla dodawania nowego zadania
    private EditText taskNameEditText; // Pole tekstowe dla nazwy zadania
    private Spinner prioritySpinner; // Spinner dla wyboru priorytetu zadania
    private TextView cancelTextView, confirmTextView; // Przycisk anuluj i zatwierdź
    private SharedPreferences sharedPreferences; // SharedPreferences do zapisywania zadań

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicjalizacja elementów interfejsu
        taskList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, taskList);
        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(adapter);

        addTaskLayout = findViewById(R.id.addTaskLayout);
        taskNameEditText = findViewById(R.id.taskNameEditText);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        cancelTextView = findViewById(R.id.cancelTextView);
        confirmTextView = findViewById(R.id.confirmTextView);

        // Ustawienie opcji dla spinnera
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        // Kliknięcie przycisku FloatingActionButton
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> addTaskLayout.setVisibility(View.VISIBLE));

        // Kliknięcie przycisku Anuluj
        cancelTextView.setOnClickListener(view -> addTaskLayout.setVisibility(View.GONE));

        // Kliknięcie przycisku Zatwierdź
        confirmTextView.setOnClickListener(view -> {
            String taskName = taskNameEditText.getText().toString();
            String priority = prioritySpinner.getSelectedItem().toString();
            if (!taskName.isEmpty() && !priority.isEmpty()) {
                String task = taskName + " (Priorytet: " + priority + ")";
                taskList.add(task);
                sortTasksByPriority(); // Sortowanie zadań według priorytetu
                adapter.notifyDataSetChanged();
                saveTasks();
                taskNameEditText.setText("");
                prioritySpinner.setSelection(0);
                addTaskLayout.setVisibility(View.GONE);
            }
        });

        // Inicjalizacja SharedPreferences do zapisywania zadań
        sharedPreferences = getSharedPreferences("TaskList", Context.MODE_PRIVATE);
        loadTasks();

        // Dodanie opcji przytrzymania elementu ListView
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showTaskOptionsDialog(position);
            return true;
        });
    }

    // Metoda zapisywania zadań do SharedPreferences
    private void saveTasks() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> taskSet = new HashSet<>(taskList);
        editor.putStringSet("tasks", taskSet);
        editor.apply();
    }

    // Metoda ładowania zadań z SharedPreferences
    private void loadTasks() {
        Set<String> taskSet = sharedPreferences.getStringSet("tasks", new HashSet<>());
        if (taskSet != null) {
            taskList.addAll(taskSet);
            sortTasksByPriority(); // Sortowanie zadań po załadowaniu
            adapter.notifyDataSetChanged();
        }
    }

    // Metoda sortująca zadania według priorytetu
    private void sortTasksByPriority() {
        Collections.sort(taskList, new Comparator<String>() {
            @Override
            public int compare(String task1, String task2) {
                String priority1 = task1.split(" \\(Priorytet: ")[1].replace(")", "");
                String priority2 = task2.split(" \\(Priorytet: ")[1].replace(")", "");

                return getPriorityValue(priority2) - getPriorityValue(priority1);
            }

            private int getPriorityValue(String priority) {
                switch (priority) {
                    case "Wysoki":
                        return 3;
                    case "Średni":
                        return 2;
                    case "Niski":
                        return 1;
                    default:
                        return 0;
                }
            }
        });
    }

    // Metoda wyświetlająca dialog z opcjami edycji i usuwania zadania
    private void showTaskOptionsDialog(int position) {
        final String[] options = {"Edytuj", "Usuń"};
        new AlertDialog.Builder(this)
                .setTitle("Opcje zadania")
                .setItems(options, (dialog, which) -> {
                    if ("Edytuj".equals(options[which])) {
                        showEditTaskDialog(position);
                    } else if ("Usuń".equals(options[which])) {
                        taskList.remove(position);
                        sortTasksByPriority(); // Sortowanie zadań po usunięciu
                        adapter.notifyDataSetChanged();
                        saveTasks();
                        Toast.makeText(MainActivity.this, "Zadanie usunięte", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    // Metoda wyświetlająca dialog do edycji zadania
    private void showEditTaskDialog(int position) {
        final String task = taskList.get(position);
        String[] parts = task.split(" \\(Priorytet: ");
        String taskName = parts[0];
        String priority = parts[1].replace(")", "");

        // Dynamicznie tworzenie widoku dialogu
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        final EditText editTaskNameEditText = new EditText(this);
        editTaskNameEditText.setHint("Nazwa zadania");
        editTaskNameEditText.setText(taskName);
        layout.addView(editTaskNameEditText);

        final Spinner editPrioritySpinner = new Spinner(this);
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editPrioritySpinner.setAdapter(priorityAdapter);
        if ("Niski".equals(priority)) {
            editPrioritySpinner.setSelection(0);
        } else if ("Średni".equals(priority)) {
            editPrioritySpinner.setSelection(1);
        } else if ("Wysoki".equals(priority)) {
            editPrioritySpinner.setSelection(2);
        }
        layout.addView(editPrioritySpinner);

        // Wyświetlanie dialogu do edycji zadania
        new AlertDialog.Builder(this)
                .setView(layout)
                .setTitle("Edytuj zadanie")
                .setPositiveButton("Zatwierdź", (dialog, which) -> {
                    String newTaskName = editTaskNameEditText.getText().toString();
                    String newPriority = editPrioritySpinner.getSelectedItem().toString();
                    if (!newTaskName.isEmpty() && !newPriority.isEmpty()) {
                        String newTask = newTaskName + " (Priorytet: " + newPriority + ")";
                        taskList.set(position, newTask);
                        sortTasksByPriority(); // Sortowanie zadań po edycji
                        adapter.notifyDataSetChanged();
                        saveTasks();
                        Toast.makeText(MainActivity.this, "Zadanie zaktualizowane", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }
}
