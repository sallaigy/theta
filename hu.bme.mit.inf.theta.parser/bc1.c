extern int __VERIFIER_nondet_int();

int main() {

	int x,y,z;

x=0;
y=0;
z=0;

while(1)
  {
    x = x +4*__VERIFIER_nondet_int();
    y = y +4*__VERIFIER_nondet_int();
    z = z +8*__VERIFIER_nondet_int();

    assert(x+y+z!=1);
  }
  return 0;
}
