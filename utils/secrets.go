package utils

import (
	"encoding/json"
	"github.com/hako/branca"
)

const SECRET_KEY = "supersecretkeyyoushouldnotcommit"  // TODO load this from config

func EncodeSecret(secret map[string]any) (string, error) {
	bytes, err := json.Marshal(secret)
	if err != nil {
		return "", err
	}

	b := branca.NewBranca(SECRET_KEY)
	token, err := b.EncodeToString(string(bytes))
	if err != nil {
		return "", err
	}

	return token, nil
}

func DecodeSecret(secret string) (map[string]any, error) {
	b := branca.NewBranca(SECRET_KEY)
	message, err := b.DecodeToString(secret)
	if err != nil {
		return nil, err
	}

	outlet := make(map[string]any)
	err = json.Unmarshal([]byte(message), &outlet)
	if err != nil {
		return nil, err
	}

	return outlet, nil
}

