package common

import (
    "github.com/essentialkaos/branca"
)

// Encode strings based on a 32 bytes long salt
func Encode(payload, salt string) (string, error) {
    brc, err := branca.NewBranca([]byte(salt))
    if err != nil {
        return "", err
    }
    token, err := brc.EncodeToString([]byte(payload))
    return token, err
}

// Decode strings based on a 32 bytes long salt
func Decode(token, salt string) (string, error) {
    brc, err := branca.NewBranca([]byte(salt))
    if err != nil {
        return "", err
    }

    brancaToken, err := brc.DecodeString(token)
    if err != nil {
        return "", err
    }

    payloadBytes := brancaToken.Payload()
    return string(payloadBytes), nil    
}

// TODO hide strings

