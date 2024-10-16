package model

type Client struct {
	Id            int    `json:"id"`
	Email         string `json:"email"`
	Password      string `json:"string"`
	IsAdmin       bool   `json:"is_admin"`
	AuthKey       string `json:"auth_key"`
	CreatedAt     string `json:"created_at"`
	LastUpdatedAt string `json:"last_updated_at"`
}

