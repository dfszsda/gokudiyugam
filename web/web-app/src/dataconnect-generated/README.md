# Generated TypeScript README
This README will guide you through the process of using the generated JavaScript SDK package for the connector `example`. It will also provide examples on how to use your generated SDK to call your Data Connect queries and mutations.

**If you're looking for the `React README`, you can find it at [`dataconnect-generated/react/README.md`](./react/README.md)**

***NOTE:** This README is generated alongside the generated SDK. If you make changes to this file, they will be overwritten when the SDK is regenerated.*

# Table of Contents
- [**Overview**](#generated-javascript-readme)
- [**Accessing the connector**](#accessing-the-connector)
  - [*Connecting to the local Emulator*](#connecting-to-the-local-emulator)
- [**Queries**](#queries)
  - [*ListAllPractices*](#listallpractices)
  - [*GetMyReflections*](#getmyreflections)
- [**Mutations**](#mutations)
  - [*CreateNewGroup*](#createnewgroup)
  - [*RecordUserPracticeCompletion*](#recorduserpracticecompletion)

# Accessing the connector
A connector is a collection of Queries and Mutations. One SDK is generated for each connector - this SDK is generated for the connector `example`. You can find more information about connectors in the [Data Connect documentation](https://firebase.google.com/docs/data-connect#how-does).

You can use this generated SDK by importing from the package `@dataconnect/generated` as shown below. Both CommonJS and ESM imports are supported.

You can also follow the instructions from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#set-client).

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig } from '@dataconnect/generated';

const dataConnect = getDataConnect(connectorConfig);
```

## Connecting to the local Emulator
By default, the connector will connect to the production service.

To connect to the emulator, you can use the following code.
You can also follow the emulator instructions from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#instrument-clients).

```typescript
import { connectDataConnectEmulator, getDataConnect } from 'firebase/data-connect';
import { connectorConfig } from '@dataconnect/generated';

const dataConnect = getDataConnect(connectorConfig);
connectDataConnectEmulator(dataConnect, 'localhost', 9399);
```

After it's initialized, you can call your Data Connect [queries](#queries) and [mutations](#mutations) from your generated SDK.

# Queries

There are two ways to execute a Data Connect Query using the generated Web SDK:
- Using a Query Reference function, which returns a `QueryRef`
  - The `QueryRef` can be used as an argument to `executeQuery()`, which will execute the Query and return a `QueryPromise`
- Using an action shortcut function, which returns a `QueryPromise`
  - Calling the action shortcut function will execute the Query and return a `QueryPromise`

The following is true for both the action shortcut function and the `QueryRef` function:
- The `QueryPromise` returned will resolve to the result of the Query once it has finished executing
- If the Query accepts arguments, both the action shortcut function and the `QueryRef` function accept a single argument: an object that contains all the required variables (and the optional variables) for the Query
- Both functions can be called with or without passing in a `DataConnect` instance as an argument. If no `DataConnect` argument is passed in, then the generated SDK will call `getDataConnect(connectorConfig)` behind the scenes for you.

Below are examples of how to use the `example` connector's generated functions to execute each query. You can also follow the examples from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#using-queries).

## ListAllPractices
You can execute the `ListAllPractices` query using the following action shortcut function, or by calling `executeQuery()` after calling the following `QueryRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
listAllPractices(): QueryPromise<ListAllPracticesData, undefined>;

interface ListAllPracticesRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<ListAllPracticesData, undefined>;
}
export const listAllPracticesRef: ListAllPracticesRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `QueryRef` function.
```typescript
listAllPractices(dc: DataConnect): QueryPromise<ListAllPracticesData, undefined>;

interface ListAllPracticesRef {
  ...
  (dc: DataConnect): QueryRef<ListAllPracticesData, undefined>;
}
export const listAllPracticesRef: ListAllPracticesRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the listAllPracticesRef:
```typescript
const name = listAllPracticesRef.operationName;
console.log(name);
```

### Variables
The `ListAllPractices` query has no variables.
### Return Type
Recall that executing the `ListAllPractices` query returns a `QueryPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `ListAllPracticesData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface ListAllPracticesData {
  practices: ({
    id: UUIDString;
    title: string;
    description: string;
    durationMinutes: number;
    type: string;
    difficultyLevel?: string | null;
    imageUrl?: string | null;
  } & Practice_Key)[];
}
```
### Using `ListAllPractices`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, listAllPractices } from '@dataconnect/generated';


// Call the `listAllPractices()` function to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await listAllPractices();

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await listAllPractices(dataConnect);

console.log(data.practices);

// Or, you can use the `Promise` API.
listAllPractices().then((response) => {
  const data = response.data;
  console.log(data.practices);
});
```

### Using `ListAllPractices`'s `QueryRef` function

```typescript
import { getDataConnect, executeQuery } from 'firebase/data-connect';
import { connectorConfig, listAllPracticesRef } from '@dataconnect/generated';


// Call the `listAllPracticesRef()` function to get a reference to the query.
const ref = listAllPracticesRef();

// You can also pass in a `DataConnect` instance to the `QueryRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = listAllPracticesRef(dataConnect);

// Call `executeQuery()` on the reference to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeQuery(ref);

console.log(data.practices);

// Or, you can use the `Promise` API.
executeQuery(ref).then((response) => {
  const data = response.data;
  console.log(data.practices);
});
```

## GetMyReflections
You can execute the `GetMyReflections` query using the following action shortcut function, or by calling `executeQuery()` after calling the following `QueryRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
getMyReflections(): QueryPromise<GetMyReflectionsData, undefined>;

interface GetMyReflectionsRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<GetMyReflectionsData, undefined>;
}
export const getMyReflectionsRef: GetMyReflectionsRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `QueryRef` function.
```typescript
getMyReflections(dc: DataConnect): QueryPromise<GetMyReflectionsData, undefined>;

interface GetMyReflectionsRef {
  ...
  (dc: DataConnect): QueryRef<GetMyReflectionsData, undefined>;
}
export const getMyReflectionsRef: GetMyReflectionsRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the getMyReflectionsRef:
```typescript
const name = getMyReflectionsRef.operationName;
console.log(name);
```

### Variables
The `GetMyReflections` query has no variables.
### Return Type
Recall that executing the `GetMyReflections` query returns a `QueryPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `GetMyReflectionsData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface GetMyReflectionsData {
  reflections: ({
    id: UUIDString;
    title: string;
    content: string;
    createdAt: TimestampString;
    isPrivate?: boolean | null;
    associatedPractice?: {
      id: UUIDString;
      title: string;
    } & Practice_Key;
  } & Reflection_Key)[];
}
```
### Using `GetMyReflections`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, getMyReflections } from '@dataconnect/generated';


// Call the `getMyReflections()` function to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await getMyReflections();

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await getMyReflections(dataConnect);

console.log(data.reflections);

// Or, you can use the `Promise` API.
getMyReflections().then((response) => {
  const data = response.data;
  console.log(data.reflections);
});
```

### Using `GetMyReflections`'s `QueryRef` function

```typescript
import { getDataConnect, executeQuery } from 'firebase/data-connect';
import { connectorConfig, getMyReflectionsRef } from '@dataconnect/generated';


// Call the `getMyReflectionsRef()` function to get a reference to the query.
const ref = getMyReflectionsRef();

// You can also pass in a `DataConnect` instance to the `QueryRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = getMyReflectionsRef(dataConnect);

// Call `executeQuery()` on the reference to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeQuery(ref);

console.log(data.reflections);

// Or, you can use the `Promise` API.
executeQuery(ref).then((response) => {
  const data = response.data;
  console.log(data.reflections);
});
```

# Mutations

There are two ways to execute a Data Connect Mutation using the generated Web SDK:
- Using a Mutation Reference function, which returns a `MutationRef`
  - The `MutationRef` can be used as an argument to `executeMutation()`, which will execute the Mutation and return a `MutationPromise`
- Using an action shortcut function, which returns a `MutationPromise`
  - Calling the action shortcut function will execute the Mutation and return a `MutationPromise`

The following is true for both the action shortcut function and the `MutationRef` function:
- The `MutationPromise` returned will resolve to the result of the Mutation once it has finished executing
- If the Mutation accepts arguments, both the action shortcut function and the `MutationRef` function accept a single argument: an object that contains all the required variables (and the optional variables) for the Mutation
- Both functions can be called with or without passing in a `DataConnect` instance as an argument. If no `DataConnect` argument is passed in, then the generated SDK will call `getDataConnect(connectorConfig)` behind the scenes for you.

Below are examples of how to use the `example` connector's generated functions to execute each mutation. You can also follow the examples from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#using-mutations).

## CreateNewGroup
You can execute the `CreateNewGroup` mutation using the following action shortcut function, or by calling `executeMutation()` after calling the following `MutationRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
createNewGroup(vars: CreateNewGroupVariables): MutationPromise<CreateNewGroupData, CreateNewGroupVariables>;

interface CreateNewGroupRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (vars: CreateNewGroupVariables): MutationRef<CreateNewGroupData, CreateNewGroupVariables>;
}
export const createNewGroupRef: CreateNewGroupRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `MutationRef` function.
```typescript
createNewGroup(dc: DataConnect, vars: CreateNewGroupVariables): MutationPromise<CreateNewGroupData, CreateNewGroupVariables>;

interface CreateNewGroupRef {
  ...
  (dc: DataConnect, vars: CreateNewGroupVariables): MutationRef<CreateNewGroupData, CreateNewGroupVariables>;
}
export const createNewGroupRef: CreateNewGroupRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the createNewGroupRef:
```typescript
const name = createNewGroupRef.operationName;
console.log(name);
```

### Variables
The `CreateNewGroup` mutation requires an argument of type `CreateNewGroupVariables`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:

```typescript
export interface CreateNewGroupVariables {
  name: string;
  type: string;
  description?: string | null;
  imageUrl?: string | null;
}
```
### Return Type
Recall that executing the `CreateNewGroup` mutation returns a `MutationPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `CreateNewGroupData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface CreateNewGroupData {
  group_insert: Group_Key;
}
```
### Using `CreateNewGroup`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, createNewGroup, CreateNewGroupVariables } from '@dataconnect/generated';

// The `CreateNewGroup` mutation requires an argument of type `CreateNewGroupVariables`:
const createNewGroupVars: CreateNewGroupVariables = {
  name: ..., 
  type: ..., 
  description: ..., // optional
  imageUrl: ..., // optional
};

// Call the `createNewGroup()` function to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await createNewGroup(createNewGroupVars);
// Variables can be defined inline as well.
const { data } = await createNewGroup({ name: ..., type: ..., description: ..., imageUrl: ..., });

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await createNewGroup(dataConnect, createNewGroupVars);

console.log(data.group_insert);

// Or, you can use the `Promise` API.
createNewGroup(createNewGroupVars).then((response) => {
  const data = response.data;
  console.log(data.group_insert);
});
```

### Using `CreateNewGroup`'s `MutationRef` function

```typescript
import { getDataConnect, executeMutation } from 'firebase/data-connect';
import { connectorConfig, createNewGroupRef, CreateNewGroupVariables } from '@dataconnect/generated';

// The `CreateNewGroup` mutation requires an argument of type `CreateNewGroupVariables`:
const createNewGroupVars: CreateNewGroupVariables = {
  name: ..., 
  type: ..., 
  description: ..., // optional
  imageUrl: ..., // optional
};

// Call the `createNewGroupRef()` function to get a reference to the mutation.
const ref = createNewGroupRef(createNewGroupVars);
// Variables can be defined inline as well.
const ref = createNewGroupRef({ name: ..., type: ..., description: ..., imageUrl: ..., });

// You can also pass in a `DataConnect` instance to the `MutationRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = createNewGroupRef(dataConnect, createNewGroupVars);

// Call `executeMutation()` on the reference to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeMutation(ref);

console.log(data.group_insert);

// Or, you can use the `Promise` API.
executeMutation(ref).then((response) => {
  const data = response.data;
  console.log(data.group_insert);
});
```

## RecordUserPracticeCompletion
You can execute the `RecordUserPracticeCompletion` mutation using the following action shortcut function, or by calling `executeMutation()` after calling the following `MutationRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
recordUserPracticeCompletion(vars: RecordUserPracticeCompletionVariables): MutationPromise<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;

interface RecordUserPracticeCompletionRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (vars: RecordUserPracticeCompletionVariables): MutationRef<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;
}
export const recordUserPracticeCompletionRef: RecordUserPracticeCompletionRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `MutationRef` function.
```typescript
recordUserPracticeCompletion(dc: DataConnect, vars: RecordUserPracticeCompletionVariables): MutationPromise<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;

interface RecordUserPracticeCompletionRef {
  ...
  (dc: DataConnect, vars: RecordUserPracticeCompletionVariables): MutationRef<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;
}
export const recordUserPracticeCompletionRef: RecordUserPracticeCompletionRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the recordUserPracticeCompletionRef:
```typescript
const name = recordUserPracticeCompletionRef.operationName;
console.log(name);
```

### Variables
The `RecordUserPracticeCompletion` mutation requires an argument of type `RecordUserPracticeCompletionVariables`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:

```typescript
export interface RecordUserPracticeCompletionVariables {
  practiceId: UUIDString;
  durationPracticedMinutes: number;
  rating?: number | null;
  userNotes?: string | null;
}
```
### Return Type
Recall that executing the `RecordUserPracticeCompletion` mutation returns a `MutationPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `RecordUserPracticeCompletionData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface RecordUserPracticeCompletionData {
  userPractice_insert: UserPractice_Key;
}
```
### Using `RecordUserPracticeCompletion`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, recordUserPracticeCompletion, RecordUserPracticeCompletionVariables } from '@dataconnect/generated';

// The `RecordUserPracticeCompletion` mutation requires an argument of type `RecordUserPracticeCompletionVariables`:
const recordUserPracticeCompletionVars: RecordUserPracticeCompletionVariables = {
  practiceId: ..., 
  durationPracticedMinutes: ..., 
  rating: ..., // optional
  userNotes: ..., // optional
};

// Call the `recordUserPracticeCompletion()` function to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await recordUserPracticeCompletion(recordUserPracticeCompletionVars);
// Variables can be defined inline as well.
const { data } = await recordUserPracticeCompletion({ practiceId: ..., durationPracticedMinutes: ..., rating: ..., userNotes: ..., });

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await recordUserPracticeCompletion(dataConnect, recordUserPracticeCompletionVars);

console.log(data.userPractice_insert);

// Or, you can use the `Promise` API.
recordUserPracticeCompletion(recordUserPracticeCompletionVars).then((response) => {
  const data = response.data;
  console.log(data.userPractice_insert);
});
```

### Using `RecordUserPracticeCompletion`'s `MutationRef` function

```typescript
import { getDataConnect, executeMutation } from 'firebase/data-connect';
import { connectorConfig, recordUserPracticeCompletionRef, RecordUserPracticeCompletionVariables } from '@dataconnect/generated';

// The `RecordUserPracticeCompletion` mutation requires an argument of type `RecordUserPracticeCompletionVariables`:
const recordUserPracticeCompletionVars: RecordUserPracticeCompletionVariables = {
  practiceId: ..., 
  durationPracticedMinutes: ..., 
  rating: ..., // optional
  userNotes: ..., // optional
};

// Call the `recordUserPracticeCompletionRef()` function to get a reference to the mutation.
const ref = recordUserPracticeCompletionRef(recordUserPracticeCompletionVars);
// Variables can be defined inline as well.
const ref = recordUserPracticeCompletionRef({ practiceId: ..., durationPracticedMinutes: ..., rating: ..., userNotes: ..., });

// You can also pass in a `DataConnect` instance to the `MutationRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = recordUserPracticeCompletionRef(dataConnect, recordUserPracticeCompletionVars);

// Call `executeMutation()` on the reference to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeMutation(ref);

console.log(data.userPractice_insert);

// Or, you can use the `Promise` API.
executeMutation(ref).then((response) => {
  const data = response.data;
  console.log(data.userPractice_insert);
});
```

