# Basic Usage

Always prioritize using a supported framework over using the generated SDK
directly. Supported frameworks simplify the developer experience and help ensure
best practices are followed.




### React
For each operation, there is a wrapper hook that can be used to call the operation.

Here are all of the hooks that get generated:
```ts
import { useListAllPractices, useGetMyReflections, useCreateNewGroup, useRecordUserPracticeCompletion } from '@dataconnect/generated/react';
// The types of these hooks are available in react/index.d.ts

const { data, isPending, isSuccess, isError, error } = useListAllPractices();

const { data, isPending, isSuccess, isError, error } = useGetMyReflections();

const { data, isPending, isSuccess, isError, error } = useCreateNewGroup(createNewGroupVars);

const { data, isPending, isSuccess, isError, error } = useRecordUserPracticeCompletion(recordUserPracticeCompletionVars);

```

Here's an example from a different generated SDK:

```ts
import { useListAllMovies } from '@dataconnect/generated/react';

function MyComponent() {
  const { isLoading, data, error } = useListAllMovies();
  if(isLoading) {
    return <div>Loading...</div>
  }
  if(error) {
    return <div> An Error Occurred: {error} </div>
  }
}

// App.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import MyComponent from './my-component';

function App() {
  const queryClient = new QueryClient();
  return <QueryClientProvider client={queryClient}>
    <MyComponent />
  </QueryClientProvider>
}
```



## Advanced Usage
If a user is not using a supported framework, they can use the generated SDK directly.

Here's an example of how to use it with the first 5 operations:

```js
import { listAllPractices, getMyReflections, createNewGroup, recordUserPracticeCompletion } from '@dataconnect/generated';


// Operation ListAllPractices: 
const { data } = await ListAllPractices(dataConnect);

// Operation GetMyReflections: 
const { data } = await GetMyReflections(dataConnect);

// Operation CreateNewGroup:  For variables, look at type CreateNewGroupVars in ../index.d.ts
const { data } = await CreateNewGroup(dataConnect, createNewGroupVars);

// Operation RecordUserPracticeCompletion:  For variables, look at type RecordUserPracticeCompletionVars in ../index.d.ts
const { data } = await RecordUserPracticeCompletion(dataConnect, recordUserPracticeCompletionVars);


```