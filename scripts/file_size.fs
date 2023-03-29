32 constant key_length
4 constant ascii_eot
10 constant line_feed
58 constant ascii_colon
variable reached?
variable temp
variable rec_key
rec_key key_length cells allot

: read_key
	0
	begin
		dup
		key dup rot cells rec_key + !
		swap 1 + dup rot dup
	ascii_colon = swap
	ascii_eot = or swap
	key_length >= or until
	drop
;

: debug_key
	key_length 0 do
		rec_key i cells + @ .
	loop
;

: is_rec_key_file_size?
	0 cells rec_key + @ 102 = \ f
	1 cells rec_key + @ 105 = \ i
	2 cells rec_key + @ 108 = \ l
	3 cells rec_key + @ 101 = \ e
	4 cells rec_key + @ 95 =  \ _
	5 cells rec_key + @ 115 = \ s
	6 cells rec_key + @ 105 = \ i
	7 cells rec_key + @ 122 = \ z
	8 cells rec_key + @ 101 = \ e
	and and and and and and and and
;

: is_rec_key_contents?
	0 cells rec_key + @ 99 =  \ c
	1 cells rec_key + @ 111 = \ o
	2 cells rec_key + @ 110 = \ n
	3 cells rec_key + @ 116 = \ t
	4 cells rec_key + @ 101 = \ e
	5 cells rec_key + @ 110 = \ n
	6 cells rec_key + @ 116 = \ t
	7 cells rec_key + @ 115 = \ s
	and and and and and and and
;

: skip_line 
	begin key line_feed = until 
;

: write_key
	-1 temp !
	begin
		1 temp +!
		temp @ cells rec_key + @
		dup emit
	ascii_colon = until
;

: bypass_key key dup emit ;

: write_value begin bypass_key line_feed = until ;

: handle_content
	key emit  \ reading the space before the contents
	-1  \ already counting the line feed that will eventually come
	begin
		1 +
		bypass_key
	line_feed = until
	." file_size: " . cr
;

: is_end_reached?
	false reached? !
	key_length 0 do
		rec_key i cells + @
		ascii_eot =
		reached? @ or
		reached? !
	loop
	reached? @
;

: clear_memory
	key_length 0 do
		0 rec_key i cells + !
	loop
;


( ######## )
( # MAIN # )
( ######## )
: setup
	clear_memory
;

: draw
	clear_memory
	read_key
	is_end_reached? if
		cr
	else is_rec_key_file_size? if
		skip_line
	else is_rec_key_contents? if
		write_key
		handle_content
	else
		write_key
		write_value
	then then then
;

: main
	setup
	begin
		draw
	is_end_reached? until
;

main

