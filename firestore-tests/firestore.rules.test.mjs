// Security-rule tests. Run from this folder with `npm install` then `npm test`
// (needs Java for the Firestore emulator). They start an emulator, load ../firestore.rules, and
// check what each kind of user may and may not do.
import { readFileSync } from 'node:fs';
import { test, before, after, beforeEach } from 'node:test';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} from '@firebase/rules-unit-testing';
import {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  serverTimestamp,
  arrayUnion,
} from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-shakeandeat',
    firestore: {
      rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
    },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

const asUser = (uid) => env.authenticatedContext(uid, { email: `${uid}@example.com` }).firestore();
const asGuest = () => env.unauthenticatedContext().firestore();

// Group ABC123 owned by alice (member: alice), with one option and no votes.
async function seedGroup(members = ['alice']) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'groups/ABC123'), {
      name: 'Lunch Crew',
      ownerUid: 'alice',
      memberUids: members,
      status: 'voting',
      createdAt: new Date(),
    });
    await setDoc(doc(db, 'groups/ABC123/options/o1'), {
      name: 'Bodo’s Bagels',
      addedByUid: 'alice',
      voteUids: [],
      createdAt: new Date(),
    });
  });
}

const newGroup = (overrides = {}) => ({
  name: 'Lunch Crew',
  ownerUid: 'alice',
  memberUids: ['alice'],
  status: 'voting',
  createdAt: serverTimestamp(),
  ...overrides,
});

const newMessage = (overrides = {}) => ({
  senderUid: 'alice',
  senderName: 'Alice',
  text: 'Bagels?',
  type: 'user',
  createdAt: serverTimestamp(),
  ...overrides,
});

// ---- users

test('signed-out visitors cannot read profiles', async () => {
  await assertFails(getDoc(doc(asGuest(), 'users/alice')));
});

test('a user can create their own profile but not someone else’s', async () => {
  const profile = (email) => ({ displayName: 'Alice', email, createdAt: serverTimestamp() });
  await assertSucceeds(setDoc(doc(asUser('alice'), 'users/alice'), profile('alice@example.com')));
  await assertFails(setDoc(doc(asUser('bob'), 'users/alice'), profile('alice@example.com')));
});

test('a profile cannot claim a different email', async () => {
  await assertFails(
    setDoc(doc(asUser('alice'), 'users/alice'), {
      displayName: 'Alice',
      email: 'someone-else@example.com',
      createdAt: serverTimestamp(),
    }),
  );
});

// ---- groups

test('a user can create a group they own, with a six-character code', async () => {
  await assertSucceeds(setDoc(doc(asUser('alice'), 'groups/ABC123'), newGroup()));
});

test('group creation rejects a spoofed owner, extra members and a bad code', async () => {
  const db = asUser('alice');
  await assertFails(setDoc(doc(db, 'groups/ABC123'), newGroup({ ownerUid: 'bob' })));
  await assertFails(setDoc(doc(db, 'groups/ABC123'), newGroup({ memberUids: ['alice', 'bob'] })));
  await assertFails(setDoc(doc(db, 'groups/abc'), newGroup()));
});

test('only members can read a group', async () => {
  await seedGroup(['alice']);
  await assertSucceeds(getDoc(doc(asUser('alice'), 'groups/ABC123')));
  await assertFails(getDoc(doc(asUser('mallory'), 'groups/ABC123')));
  await assertFails(getDoc(doc(asGuest(), 'groups/ABC123')));
});

test('someone with the code can join as themselves but cannot add another person', async () => {
  await seedGroup(['alice']);
  await assertFails(updateDoc(doc(asUser('bob'), 'groups/ABC123'), { memberUids: arrayUnion('carol') }));
  await assertSucceeds(updateDoc(doc(asUser('bob'), 'groups/ABC123'), { memberUids: arrayUnion('bob') }));
});

test('a non-member cannot change the decision or delete the group', async () => {
  await seedGroup(['alice']);
  const db = asUser('mallory');
  await assertFails(updateDoc(doc(db, 'groups/ABC123'), { status: 'decided' }));
  await assertFails(deleteDoc(doc(db, 'groups/ABC123')));
});

test('a member can record the decision; only the owner can delete', async () => {
  await seedGroup(['alice', 'bob']);
  await assertSucceeds(
    updateDoc(doc(asUser('bob'), 'groups/ABC123'), { status: 'decided', winnerOptionId: 'o1' }),
  );
  await assertFails(deleteDoc(doc(asUser('bob'), 'groups/ABC123')));
  await assertSucceeds(deleteDoc(doc(asUser('alice'), 'groups/ABC123')));
});

// ---- options and votes

test('members can add an option as themselves, and non-members cannot', async () => {
  await seedGroup(['alice', 'bob']);
  const option = (uid) => ({
    name: 'Roots',
    addedByUid: uid,
    voteUids: [],
    createdAt: serverTimestamp(),
  });
  await assertSucceeds(setDoc(doc(asUser('bob'), 'groups/ABC123/options/o2'), option('bob')));
  await assertFails(setDoc(doc(asUser('bob'), 'groups/ABC123/options/o3'), option('alice')));
  await assertFails(setDoc(doc(asUser('mallory'), 'groups/ABC123/options/o4'), option('mallory')));
});

test('a member can vote for themselves but not for someone else', async () => {
  await seedGroup(['alice', 'bob']);
  const option = doc(asUser('bob'), 'groups/ABC123/options/o1');
  await assertFails(updateDoc(option, { voteUids: arrayUnion('alice') }));
  await assertSucceeds(updateDoc(option, { voteUids: arrayUnion('bob') }));
});

test('non-members cannot read or vote on options', async () => {
  await seedGroup(['alice']);
  const option = doc(asUser('mallory'), 'groups/ABC123/options/o1');
  await assertFails(getDoc(option));
  await assertFails(updateDoc(option, { voteUids: arrayUnion('mallory') }));
});

// ---- messages

test('members can post as themselves; impersonation is rejected', async () => {
  await seedGroup(['alice', 'bob']);
  await assertSucceeds(setDoc(doc(asUser('bob'), 'groups/ABC123/messages/m1'), newMessage({ senderUid: 'bob' })));
  await assertFails(setDoc(doc(asUser('bob'), 'groups/ABC123/messages/m2'), newMessage({ senderUid: 'alice' })));
});

test('messages are limited to 500 characters and cannot be edited or deleted', async () => {
  await seedGroup(['alice']);
  const db = asUser('alice');
  await assertFails(setDoc(doc(db, 'groups/ABC123/messages/long'), newMessage({ text: 'x'.repeat(501) })));
  await assertSucceeds(setDoc(doc(db, 'groups/ABC123/messages/m1'), newMessage()));
  await assertFails(updateDoc(doc(db, 'groups/ABC123/messages/m1'), { text: 'edited' }));
  await assertFails(deleteDoc(doc(db, 'groups/ABC123/messages/m1')));
});

test('only members can read messages', async () => {
  await seedGroup(['alice']);
  await assertSucceeds(setDoc(doc(asUser('alice'), 'groups/ABC123/messages/m1'), newMessage()));
  await assertSucceeds(getDoc(doc(asUser('alice'), 'groups/ABC123/messages/m1')));
  await assertFails(getDoc(doc(asUser('mallory'), 'groups/ABC123/messages/m1')));
});

// ---- default deny

test('anything not named in the rules is denied', async () => {
  await assertFails(setDoc(doc(asUser('alice'), 'surprise/doc'), { hello: 'world' }));
});
