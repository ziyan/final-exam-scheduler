drop table if exists obj_section;
create table if not exists obj_section (
	id char(11) not null primary key,
	title char(100) not null,
	professor char(50) not null,
	status int not null,
	current int not null,
	max int not null,
	time char(10) not null
);

drop table if exists rel_section_student;
create table if not exists rel_section_student (
	section_id char(11) not null,
	student_id char(10) not null,
	primary key (section_id, student_id),
	foreign key (section_id) references obj_section(id) on delete cascade,
	foreign key (student_id) references obj_student(id) on delete cascade
);

drop table if exists obj_student;
create table if not exists obj_student(
	id char(10) not null primary key,
	name char(50) not null
);
