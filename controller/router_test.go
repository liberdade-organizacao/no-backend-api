package controller

import (
	"testing"
	"fmt"
)

func quickCompareMaps(a, b map[string]any) bool {
	return fmt.Sprintf("%#v", a) == fmt.Sprintf("%#v", b)
}

func TestParseJson(t *testing.T) {
	// good case
	expectedResult := map[string]any {
		"message": "hello",
		"count": 1,
	}
	contents := []byte(`{"message": "hello", "count": 1}`)
	obtainedResult, err := ParseJson(contents)
	if err != nil {
		t.Fatalf("failed to parse json: %#v\n", err)
		return
	}
	if !quickCompareMaps(expectedResult, obtainedResult) {
		t.Fatalf("parsed objects wrongly:\n expected: %#v\nobtained: %#v\n", expectedResult, obtainedResult)
		return
	}

	// bad case
	contents = []byte(`{"incomplete`) 
	_, err = ParseJson(contents)
	if err == nil {
		t.Fatal("parsed broken json")
		return
	}
}

func TestWriteJson(t *testing.T) {
	expectedResult := `{"age":1000,"hobbies":["bass"],"name":"Marceline"}`
	obj := map[string]any {
		"name": "Marceline",
		"age": 1000,
		"hobbies": []any {
			"bass",
		},
	}
	obtainedResult, err := WriteJson(obj)
	if err != nil {
		t.Fatalf("Failed to write JSONL: %s", err)
		return
	}
	if expectedResult != obtainedResult {
		t.Fatalf("Wrote wrong JSON\nexpected: %s\nobtained: %s", expectedResult, obtainedResult)
		return
	}
}

